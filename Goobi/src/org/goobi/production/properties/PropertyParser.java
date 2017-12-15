package org.goobi.production.properties;

/**
 * This file is part of the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information. 
 *          - http://www.goobi.org
 *          - http://launchpad.net/goobi-production
 *          - http://gdz.sub.uni-goettingen.de
 *          - http://www.intranda.com
 *          - http://digiverso.com 
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * Linking this library statically or dynamically with other modules is making a combined work based on this library. Thus, the terms and conditions
 * of the GNU General Public License cover the whole combination. As a special exception, the copyright holders of this library give you permission to
 * link this library with independent modules to produce an executable, regardless of the license terms of these independent modules, and to copy and
 * distribute the resulting executable under terms of your choice, provided that you also meet, for each linked independent module, the terms and
 * conditions of the license of that module. An independent module is a module which is not derived from or based on this library. If you modify this
 * library, you may extend this exception to your version of the library, but you are not obliged to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.log4j.Logger;

import org.goobi.beans.Process;
import org.goobi.beans.Processproperty;
import org.goobi.beans.Step;


import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.persistence.managers.MetadataManager;

public class PropertyParser {
    private static final Logger logger = Logger.getLogger(PropertyParser.class);

    public static void main(String[] args) {
        PropertyParser parser = new PropertyParser();
        parser.readConfigAsSample();
    }

    public static ArrayList<ProcessProperty> getPropertiesForStep(Step mySchritt) {

        String stepTitle = mySchritt.getTitel();
        String projectTitle = mySchritt.getProzess().getProjekt().getTitel();
        String workflowTitle = "";
        ArrayList<ProcessProperty> properties = new ArrayList<ProcessProperty>();

        // find out original workflow template
        for (Processproperty p : mySchritt.getProzess().getEigenschaften()) {
        	if (p.getTitel().equals("Template")){
        		workflowTitle = p.getWert();
        	}
		}
        
        if (mySchritt.getProzess().isIstTemplate()) {
            return properties;
        }

        String path = ConfigurationHelper.getInstance().getConfigurationFolder() + "goobi_processProperties.xml";
        XMLConfiguration config;
        try {
            config = new XMLConfiguration(path);
        } catch (ConfigurationException e) {
            logger.error(e);
            config = new XMLConfiguration();
        }
        config.setListDelimiter('&');
        config.setReloadingStrategy(new FileChangedReloadingStrategy());

        // run though all properties
        int countProperties = config.getMaxIndex("property");
        for (int i = 0; i <= countProperties; i++) {

            // general values for property
            ProcessProperty pp = new ProcessProperty();
            pp.setName(config.getString("property(" + i + ")[@name]"));
            pp.setContainer(config.getInt("property(" + i + ")[@container]"));

            // projects
            int count = config.getMaxIndex("property(" + i + ").project");
            for (int j = 0; j <= count; j++) {
                pp.getProjects().add(config.getString("property(" + i + ").project(" + j + ")"));
            }
            
            // workflows
            count = config.getMaxIndex("property(" + i + ").workflow");
            for (int j = 0; j <= count; j++) {
                pp.getWorkflows().add(config.getString("property(" + i + ").workflow(" + j + ")"));
            }

            // project and workflows are configured correct?
            boolean projectOk = pp.getProjects().contains("*") || pp.getProjects().contains(projectTitle) || pp.getProjects().size()==0;
            boolean workflowOk = pp.getWorkflows().contains("*") || pp.getWorkflows().contains(workflowTitle) || pp.getWorkflows().size()==0;
            
            if (projectOk && workflowOk) {

                // showStep
                boolean containsCurrentStepTitle = false;
                count = config.getMaxIndex("property(" + i + ").showStep");
                for (int j = 0; j <= count; j++) {
                    ShowStepCondition ssc = new ShowStepCondition();
                    ssc.setName(config.getString("property(" + i + ").showStep(" + j + ")[@name]"));
                    String access = config.getString("property(" + i + ").showStep(" + j + ")[@access]");
                    boolean duplicate = config.getBoolean("property(" + i + ").showStep(" + j + ")[@duplicate]", false);
                    ssc.setAccessCondition(AccessCondition.getAccessConditionByName(access));
                    if (ssc.getName().equals(stepTitle) || ssc.getName().equals("*")) {
                        containsCurrentStepTitle = true;
                        pp.setDuplicationAllowed(duplicate);
                        pp.setCurrentStepAccessCondition(AccessCondition.getAccessConditionByName(access));
                    }

                    pp.getShowStepConditions().add(ssc);
                }

                // steptitle is configured
                if (containsCurrentStepTitle) {
                    // showProcessGroupAccessCondition
                    String groupAccess = config.getString("property(" + i + ").showProcessGroup[@access]");
                    if (groupAccess != null) {
                        pp.setShowProcessGroupAccessCondition(AccessCondition.getAccessConditionByName(groupAccess));
                    } else {
                        pp.setShowProcessGroupAccessCondition(AccessCondition.WRITE);
                    }

                    // validation expression
                    pp.setValidation(config.getString("property(" + i + ").validation"));
                    // type
                    pp.setType(Type.getTypeByName(config.getString("property(" + i + ").type")));
                    // (default) value
                    String defaultValue = config.getString("property(" + i + ").defaultvalue");
                    if (pp.getType().equals(Type.METADATA)) {
                        String metadata = MetadataManager.getMetadataValue(mySchritt.getProzess().getId(), defaultValue);
                        pp.setValue(metadata);
                    } else {
                        pp.setValue(defaultValue);
                        pp.setReadValue("");
                    }

                    // possible values
                    count = config.getMaxIndex("property(" + i + ").value");
                    for (int j = 0; j <= count; j++) {
                        pp.getPossibleValues().add(config.getString("property(" + i + ").value(" + j + ")"));
                    }
                    properties.add(pp);
                }
            }
        }

        // add existing 'eigenschaften' to properties from config, so we have all properties from config and some of them with already existing
        // 'eigenschaften'
        ArrayList<ProcessProperty> listClone = new ArrayList<ProcessProperty>(properties);
        mySchritt.getProzess().setEigenschaften(null);
        List<Processproperty> plist = mySchritt.getProzess().getEigenschaftenList();
        for (Processproperty pe : plist) {

            for (ProcessProperty pp : listClone) {
                if (pe.getTitel() != null) {

                    if (pe.getTitel().equals(pp.getName())) {
                        // pp has no pe assigned
                        if (pp.getProzesseigenschaft() == null) {
                            pp.setProzesseigenschaft(pe);
                            pp.setValue(pe.getWert());
                            pp.setContainer(pe.getContainer());
                        } else {
                            // clone pp
                            ProcessProperty pnew = pp.getClone(pe.getContainer());
                            pnew.setProzesseigenschaft(pe);
                            pnew.setValue(pe.getWert());
                            pnew.setContainer(pe.getContainer());
                            properties.add(pnew);
                        }
                    }
                }
            }
        }
        return properties;
    }

    public static List<ProcessProperty> getPropertiesForProcess(Process process) {
        //      Hibernate.initialize(process.getProjekt());
        String projectTitle = process.getProjekt().getTitel();
        ArrayList<ProcessProperty> properties = new ArrayList<ProcessProperty>();
        if (process.isIstTemplate()) {
            List<Processproperty> plist = process.getEigenschaftenList();
            for (Processproperty pe : plist) {
                ProcessProperty pp = new ProcessProperty();
                pp.setName(pe.getTitel());
                pp.setProzesseigenschaft(pe);
                pp.setType(Type.TEXT);
                pp.setValue(pe.getWert());
                pp.setContainer(pe.getContainer());
                properties.add(pp);
            }
            return properties;
        }
        String path = ConfigurationHelper.getInstance().getConfigurationFolder() + "goobi_processProperties.xml";
        XMLConfiguration config;
        try {
            config = new XMLConfiguration(path);
        } catch (ConfigurationException e) {
            logger.error(e);
            config = new XMLConfiguration();
        }
        config.setListDelimiter('&');
        config.setReloadingStrategy(new FileChangedReloadingStrategy());

        // run though all properties
        int countProperties = config.getMaxIndex("property");
        for (int i = 0; i <= countProperties; i++) {

            // general values for property
            ProcessProperty pp = new ProcessProperty();
            pp.setName(config.getString("property(" + i + ")[@name]"));
            pp.setContainer(config.getInt("property(" + i + ")[@container]"));

            // projects
            int count = config.getMaxIndex("property(" + i + ").project");
            for (int j = 0; j <= count; j++) {
                pp.getProjects().add(config.getString("property(" + i + ").project(" + j + ")"));
            }

            // project is configured
            if (pp.getProjects().contains("*") || pp.getProjects().contains(projectTitle)) {
                String groupAccess = config.getString("property(" + i + ").showProcessGroup[@access]");
                if (groupAccess != null) {
                    pp.setShowProcessGroupAccessCondition(AccessCondition.getAccessConditionByName(groupAccess));
                } else {
                    pp.setShowProcessGroupAccessCondition(AccessCondition.WRITE);
                }
                // validation expression
                pp.setValidation(config.getString("property(" + i + ").validation"));
                // type
                pp.setType(Type.getTypeByName(config.getString("property(" + i + ").type")));
                // (default) value
                String defaultValue = config.getString("property(" + i + ").defaultvalue");
                if (pp.getType().equals(Type.METADATA)) {
                    String metadata = MetadataManager.getMetadataValue(process.getId(), defaultValue);
                    pp.setValue(metadata);
                } else {
                    pp.setValue(defaultValue);
                    pp.setReadValue("");
                }

                // possible values
                count = config.getMaxIndex("property(" + i + ").value");
                for (int j = 0; j <= count; j++) {
                    pp.getPossibleValues().add(config.getString("property(" + i + ").value(" + j + ")"));
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("add property A " + pp.getName() + " - " + pp.getValue() + " - " + pp.getContainer());
                }
                properties.add(pp);

            }
        } // add existing 'eigenschaften' to properties from config, so we have all properties from config and some of them with already existing
          // 'eigenschaften'
        List<ProcessProperty> listClone = new ArrayList<ProcessProperty>(properties);
        process.setEigenschaften(null);
        List<Processproperty> plist = process.getEigenschaftenList();
        for (Processproperty pe : plist) {

            if (pe.getTitel() != null) {

                for (ProcessProperty pp : listClone) {
                    if (pe.getTitel().equals(pp.getName())) {
                        // pp has no pe assigned
                        if (pp.getProzesseigenschaft() == null) {
                            pp.setProzesseigenschaft(pe);
                            pp.setValue(pe.getWert());
                            pp.setContainer(pe.getContainer());
                        } else {
                            // clone pp
                            ProcessProperty pnew = pp.getClone(pe.getContainer());
                            pnew.setProzesseigenschaft(pe);
                            pnew.setValue(pe.getWert());
                            pnew.setContainer(pe.getContainer());
                            if (logger.isDebugEnabled()) {
                                logger.debug("add property B " + pp.getName() + " - " + pp.getValue() + " - " + pp.getContainer());
                            }
                            properties.add(pnew);
                        }
                    }
                }
            }
        }

        // add 'eigenschaft' to all ProcessProperties
        for (ProcessProperty pp : properties) {
            if (pp.getProzesseigenschaft() == null) {
            } else {
                plist.remove(pp.getProzesseigenschaft());
            }
        }
        // create ProcessProperties to remaining 'eigenschaften'
        if (plist.size() > 0) {
            for (Processproperty pe : plist) {
                ProcessProperty pp = new ProcessProperty();
                pp.setProzesseigenschaft(pe);
                pp.setName(pe.getTitel());
                pp.setValue(pe.getWert());
                pp.setContainer(pe.getContainer());
                pp.setType(Type.TEXT);
                if (logger.isDebugEnabled()) {
                    logger.debug("add property C " + pp.getName() + " - " + pp.getValue() + " - " + pp.getContainer());
                }
                properties.add(pp);

            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("all properties are " + properties.size());
        }

        return properties;
    }

    private void readConfigAsSample() {
        ArrayList<ProcessProperty> properties = new ArrayList<ProcessProperty>();

        String path = ConfigurationHelper.getInstance().getConfigurationFolder() + "goobi_processProperties.xml";
        XMLConfiguration config;
        try {
            config = new XMLConfiguration(path);
        } catch (ConfigurationException e) {
            logger.error(e);
            config = new XMLConfiguration();
        }
        config.setListDelimiter('&');
        config.setReloadingStrategy(new FileChangedReloadingStrategy());

        // run though all properties
        int countProperties = config.getMaxIndex("property");
        for (int i = 0; i <= countProperties; i++) {

            // general values for property
            ProcessProperty pp = new ProcessProperty();
            pp.setName(config.getString("property(" + i + ")[@name]"));
            pp.setContainer(config.getInt("property(" + i + ")[@container]"));

            // projects
            int count = config.getMaxIndex("property(" + i + ").project");
            for (int j = 0; j <= count; j++) {
                pp.getProjects().add(config.getString("property(" + i + ").project(" + j + ")"));
            }

            // showStep
            count = config.getMaxIndex("property(" + i + ").showStep");
            for (int j = 0; j <= count; j++) {
                ShowStepCondition ssc = new ShowStepCondition();
                ssc.setName(config.getString("property(" + i + ").showStep(" + j + ")[@name]"));
                String access = config.getString("property(" + i + ").showStep(" + j + ")[@access]");
                boolean duplicate = config.getBoolean("property(" + i + ").showStep(" + j + ")[@duplicate]", false);
                ssc.setAccessCondition(AccessCondition.getAccessConditionByName(access));
                ssc.setDuplication(duplicate);
                pp.getShowStepConditions().add(ssc);
            }

            // showProcessGroupAccessCondition
            String groupAccess = config.getString("property(" + i + ").showProcessGroup[@access]");
            pp.setShowProcessGroupAccessCondition(AccessCondition.getAccessConditionByName(groupAccess));

            // validation expression
            pp.setValidation(config.getString("property(" + i + ").validation"));
            // type
            pp.setType(Type.getTypeByName(config.getString("property(" + i + ").type")));
            // (default) value
            pp.setValue(config.getString("property(" + i + ").defaultvalue"));

            // possible values
            count = config.getMaxIndex("property(" + i + ").value");
            for (int j = 0; j <= count; j++) {
                pp.getPossibleValues().add(config.getString("property(" + i + ").value(" + j + ")"));
            }
            properties.add(pp);
        }
    }
}
