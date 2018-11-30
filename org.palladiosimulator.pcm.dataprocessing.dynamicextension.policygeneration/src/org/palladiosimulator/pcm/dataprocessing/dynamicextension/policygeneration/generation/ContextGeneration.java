package org.palladiosimulator.pcm.dataprocessing.dynamicextension.policygeneration.generation;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.palladiosimulator.pcm.dataprocessing.dataprocessing.DataSpecification;
import org.palladiosimulator.pcm.dataprocessing.dataprocessing.characteristics.RelatedCharacteristics;
import org.palladiosimulator.pcm.dataprocessing.dynamicextension.DynamicSpecification;
import org.palladiosimulator.pcm.dataprocessing.dynamicextension.context.Context;
import org.palladiosimulator.pcm.dataprocessing.dynamicextension.context.ContextCharacteristic;
import org.palladiosimulator.pcm.dataprocessing.dynamicextension.context.IntegerThresholdContext;
import org.palladiosimulator.pcm.dataprocessing.dynamicextension.context.InternalStateContext;
import org.palladiosimulator.pcm.dataprocessing.dynamicextension.context.LocationContext;
import org.palladiosimulator.pcm.dataprocessing.dynamicextension.context.OrganisationContext;
import org.palladiosimulator.pcm.dataprocessing.dynamicextension.context.PrivacyLevelContext;
import org.palladiosimulator.pcm.dataprocessing.dynamicextension.context.RoleContext;
import org.palladiosimulator.pcm.dataprocessing.dynamicextension.context.ShiftCheckContext;
import org.palladiosimulator.pcm.dataprocessing.dynamicextension.context.ShiftContext;
import org.palladiosimulator.pcm.dataprocessing.dynamicextension.policygeneration.util.ScalaHelper;
import org.palladiosimulator.pcm.dataprocessing.dynamicextension.util.helperattributes.Location;
import org.palladiosimulator.pcm.dataprocessing.dynamicextension.util.helperattributes.Role;
import org.palladiosimulator.pcm.dataprocessing.dynamicextension.util.subject.Organisation;
import org.palladiosimulator.pcm.dataprocessing.dynamicextension.util.subject.Resource;

public class ContextGeneration {
	private DataSpecification dataContainer;
	private DynamicSpecification dynamicContainer;

	public ContextGeneration(DataSpecification dataContainer, DynamicSpecification dynamicContainer) {
		this.dataContainer = dataContainer;
		this.dynamicContainer = dynamicContainer;
		dataContainer.getCharacteristicTypeContainers();
	}
	
	
	/*
	 * Scalabilit test
	 * 
	 * write to /dev/null
	 * test each container in 10 + 1000 + 10000 + 1000000 
	 * multiple time write in end file
	 *
	 */

	public List<String> generateRelatedContexts(DataSpecification rootData, PrintWriter writer) throws IOException {
		var list = new ArrayList<String>(rootData.getRelatedCharacteristics().size());
		for(long x = 0; x <100000000; x++)
		rootData.getRelatedCharacteristics().stream().forEach(e -> {
			writer.append("class ");
			String nameEnsemble = ScalaHelper.createIdentifier(e.getRelatedEntity().getEntityName() + e.getId());
			writer.append(nameEnsemble);
			boolean shiftCheck = getContexts(e).filter(ShiftCheckContext.class::isInstance).findAny().isPresent();
			if (shiftCheck) {
				writer.append("(val shift: Shift)");
				nameEnsemble += "-S";
			} else
				writer.append("() ");
			list.add(nameEnsemble);
			writer.append(ScalaHelper.KEYWORD_EXTENDS);
			writer.append(" ");
			writer.append(ScalaHelper.KEYWORD_ENSEMBLE);
			writer.append("{\n");
			var listRoles = new ArrayList<String>();
			List<InternalStateContext> listStates = getContexts(e).filter(InternalStateContext.class::isInstance)
					.map(InternalStateContext.class::cast).collect(Collectors.toList());
			List<OrganisationContext> listOrganisationContext = getContexts(e)
					.filter(OrganisationContext.class::isInstance).map(OrganisationContext.class::cast)
					.collect(Collectors.toList());
			if (!listStates.isEmpty()) {
				writeOrganisationContext(writer, listOrganisationContext, listStates);
				listRoles.add("companies");
			}
			List<LocationContext> listLocationContext = getContexts(e).filter(LocationContext.class::isInstance)
					.map(LocationContext.class::cast).collect(Collectors.toList());
			List<RoleContext> listRolesContext = getContexts(e).filter(RoleContext.class::isInstance)
					.map(RoleContext.class::cast).collect(Collectors.toList());
			if (!listLocationContext.isEmpty() || !listRolesContext.isEmpty())
				writePersonalContext(writer, listLocationContext, listRolesContext, listOrganisationContext,
						shiftCheck);
			if (!listRolesContext.isEmpty())
				listRoles.add("persons");
			writeCPSContext(writer, listStates);
			List<PrivacyLevelContext> listPrivacyContext = getContexts(e).filter(PrivacyLevelContext.class::isInstance)
					.map(PrivacyLevelContext.class::cast).collect(Collectors.toList());
			if (!listPrivacyContext.isEmpty()) {
				writePrivacyContext(writer, listPrivacyContext);
			}
			List<ShiftContext> listShiftsContext = getContexts(e).filter(ShiftContext.class::isInstance)
					.map(ShiftContext.class::cast).collect(Collectors.toList());
			if (!listShiftsContext.isEmpty())
				writeShiftContext(writer, listShiftsContext);
			if(shiftCheck)
				writeShiftCheck(writer);
			if (listRoles.size() >= 1) {
				if (listPrivacyContext.isEmpty())
					writer.println(writeAllow(nameEnsemble, listRoles.get(0), listRoles.get(0)));
				else
					writer.println(writeAllow(nameEnsemble, listRoles.get(0), listRoles.get(0),
							listPrivacyContext.get(0).getLevel().getEntityName()));
			}
			writer.append("\n}\n");

		});
		return list;
	}

	private void writePrivacyContext(PrintWriter writer, List<PrivacyLevelContext> listPrivacyContext) {
		writer.println(listPrivacyContext.stream()
				.map(e -> String.format("x.level == PrivacyLevel.%s", e.getLevel().getEntityName()))
				.collect(Collectors.joining(" || ",
						"val levels = role(\"levels\", components.select[File].filter ( x => (", ")))")));

	}

	private Stream<Context> getContexts(RelatedCharacteristics e) {
		return e.getCharacteristics().getOwnedCharacteristics().stream().filter(ContextCharacteristic.class::isInstance)
				.map(ContextCharacteristic.class::cast).flatMap(i -> i.getContext().stream());
	}

	private String writeAllow(String nameAction, String variableName, String varbiableName2) {
		return writeAllow(nameAction, variableName, varbiableName2, null);
	}

	private String writeAllow(String nameAction, String variableName, String variableName2, String privacyLevel) {
		var t = new StringBuilder();
		t.append("allow(");
		t.append(String.format("%s,%s", variableName, variableName));
		t.append(",\"");
		t.append(nameAction);
		t.append("\"");
		t.append(privacyLevel == null ? "" : String.format(", PrivacyLevel.%s", privacyLevel));
		t.append(")");
		return t.toString();
	}

	private void writeCPSContext(PrintWriter writer, List<InternalStateContext> listStates) {
		List<InternalStateContext> relevant = listStates.stream().filter(e -> Resource.class.isInstance(e.getSubject()))
				.collect(Collectors.toList());
		if (relevant.isEmpty())
			return;
		writer.println(String.format(
				"val machines = role (\"machines\", components.select[Machine].filter( x => (x.name == \"%s\" &&  x.status.isInstanceOf[%s])))",
				ScalaHelper.createIdentifier(relevant.get(0).getSubject().getEntityName()),
				ScalaHelper.createIdentifier(
						relevant.get(0).getState().getEntityName() + relevant.get(0).getState().getId())));

	}

	private void writeOrganisationContext(PrintWriter printer, List<OrganisationContext> contexts,
			List<InternalStateContext> listStates) {
		printer.println(listStates.stream().filter(e -> Organisation.class.isInstance(e.getSubject()))
				.map(e -> String.format("(x.name == \"Company %s\" && x.status.isInstanceOf[%s])",
						e.getSubject().getEntityName(),
						ScalaHelper.createIdentifier(e.getState().getEntityName() + e.getState().getId())))
				.collect(Collectors.joining(" || ",
						"val companies = role(\"companies\",components.select[Company].filter( x => (", ")))")));

	}

	private void writePersonalContext(PrintWriter printer, List<LocationContext> locationContexts,
			List<RoleContext> roleContexts, List<OrganisationContext> listOrganisationContexts, boolean shiftCheck) {
		var s = new StringBuilder();
		s.append("\n");
		s.append("val persons = role(\"persons\",components.select[Person].filter( x => ((");
		if (!locationContexts.isEmpty()) {
			for (LocationContext context : locationContexts) {
				generateLocation(s, context.getCurrentLocation());
				List<Location> list = context.getCurrentLocation().getIncludes().stream()
						.flatMap(e -> e.getIncludes().stream()).collect(Collectors.toList());
				for (Location location : list) {
					generateLocation(s, location);
				}
			}
			s.delete(s.length() - 4, s.length());
			if (!roleContexts.isEmpty())
				s.append(") && (");
		}
		if (!roleContexts.isEmpty()) {

			for (RoleContext context : roleContexts) {
				generateRoleCheck(s, context.getRole());
				context.getRole().getSubordinateroles().stream().flatMap(e -> e.getSubordinateroles().stream())
						.forEach(role -> generateRoleCheck(s, role));
			}
			s.delete(s.length() - 4, s.length());
			if (!listOrganisationContexts.isEmpty())
				s.append(") && (");
		}
		if (!listOrganisationContexts.isEmpty()) {
			s.append(listOrganisationContexts.stream()
					.map(e -> String.format("x.company.name == \"Company %s\"", e.getOrganisation().getEntityName()))
					.collect(Collectors.joining(" || ", "", ") || ")));
			s.delete(s.length() - 4, s.length());
			if (shiftCheck)
				s.append(") && (");
		}
		if (shiftCheck) {
			s.append("x.shift.name == shift.name");
		}
		s.append(")))");
		printer.println(s.toString());
	}

	private void generateRoleCheck(StringBuilder s, Role role) {
		s.append(String.format("(x.roles.exists( k => k.isInstanceOf[%s])) || ",
				ScalaHelper.createIdentifier(role.getEntityName())));
//		s.append("x.hasRole{\n\t case ");
//		s.append(ScalaHelper.createIdentifier(role.getEntityName()));
//		s.append("(organisation) => organisation.name == \"Company ");
//		s.append(((RoleContainer) role.eContainer()).getOrganisation().getEntityName());
//		s.append("\"} || ");
	}

	private void generateLocation(StringBuilder s, Location location) {
		s.append("x.location == ");
		s.append("\"");
		s.append(location.getEntityName());
		s.append("\"");
		s.append(" || ");
	}

	private void writeShiftCheck(PrintWriter printer) {
		printer.println(
				"val shift_current = role(\"shift_current\", components.select[Shift].filter( x => ((x.name == shift.name ))))");
		printer.println("    membership(\n" + 
				"      persons.all(p => shift_current.containsOnly(p.shift))\n" + 
				"    )");
	}

	private void writeShiftContext(PrintWriter printer, List<ShiftContext> contexts) {
		var writer = new StringBuilder();
		writer.append("\n");
		writer.append("val shifts = role(\"shift\",components.select[Shift].filter( x => (");
		for (ShiftContext context : contexts) {
			writer.append("(x.name == ");
			writer.append("\"");
			writer.append(context.getEntityName());
			writer.append("\"");
			writer.append(" )|| ");
		}
		writer.delete(writer.length() - 3, writer.length());
		writer.append(")))");
		printer.println(writer.toString());
	}

	/**
	 * @return the dataContainer
	 */
	public final DataSpecification getDataContainer() {
		return dataContainer;
	}

	/**
	 * @return the dynamicContainer
	 */
	public final DynamicSpecification getDynamicContainer() {
		return dynamicContainer;
	}

}
