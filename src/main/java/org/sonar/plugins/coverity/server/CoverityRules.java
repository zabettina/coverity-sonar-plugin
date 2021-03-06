/*
 * Coverity Sonar Plugin
 * Copyright (c) 2014 Coverity, Inc
 * support@coverity.com
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 */

package org.sonar.plugins.coverity.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Extension;
//import org.sonar.api.rules.RuleRepository;
//import org.sonar.api.rules.XMLRuleParser;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionXmlLoader;
import org.sonar.plugins.coverity.CoverityPlugin;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* From Sonarqube-4.3+ the interface RulesDefinition replaces the (previously deprecated and currently dropped) RulesRepository.
 * This class loads rules into the server by means of an XmlLoader. However we still need to activate these rules under
 * a profile and then again in CoveritySensor.
 */

public class CoverityRules implements RulesDefinition, Extension {

    private RulesDefinitionXmlLoader xmlLoader = new RulesDefinitionXmlLoader();
    private static final Logger LOG = LoggerFactory.getLogger(CoverityRules.class);

    public CoverityRules(RulesDefinitionXmlLoader xmlLoader) {
        this.xmlLoader = xmlLoader;
    }

    Map<String, NodeList> mapOfNodeLists = new HashMap<String, NodeList>();

    NodeList javaNodes;
    NodeList cppNodes;
    NodeList csNodes;

    static List<String> languages = new ArrayList<String>();
    static{
        languages.add("java");
        languages.add("cpp");
        languages.add("cs");
    }

    public static Map<String, org.sonar.api.rules.Rule> javaRulesToBeActivated = new HashMap<String, org.sonar.api.rules.Rule>();
    public static Map<String, org.sonar.api.rules.Rule> cppRulesToBeActivated = new HashMap<String, org.sonar.api.rules.Rule>();
    public static Map<String, org.sonar.api.rules.Rule> cRulesToBeActivated = new HashMap<String, org.sonar.api.rules.Rule>();
    public static Map<String, org.sonar.api.rules.Rule> cppComunityRulesToBeActivated = new HashMap<String, org.sonar.api.rules.Rule>();
    public static Map<String, org.sonar.api.rules.Rule> csRulesToBeActivated = new HashMap<String, org.sonar.api.rules.Rule>();

    public static Map<String, Map<String, org.sonar.api.rules.Rule>> getMapOfRuleMaps() {
        return mapOfRuleMaps;
    }

    public static Map<String, Map<String, org.sonar.api.rules.Rule>> mapOfRuleMaps = new HashMap<String, Map<String, org.sonar.api.rules.Rule>>();

    static {
        mapOfRuleMaps.put("java", javaRulesToBeActivated);
        mapOfRuleMaps.put("cpp", cppRulesToBeActivated);
        mapOfRuleMaps.put("c++", cppComunityRulesToBeActivated);
        mapOfRuleMaps.put("c", cRulesToBeActivated);
        mapOfRuleMaps.put("cs", csRulesToBeActivated);
    }

    public CoverityRules() {
    }

    /* The interface RulesDefinition provides a default parser: "XmlLoader". However, XmlLoader stores rules as
    *  "NewRules" a class that does not provides getters for certain fields such as severity. We need to access these
    *  fields later on when activating rules in CoverityProfiles. So in order to have more control over our rules we
    *  define "InternalRule.class" and we complete its fields by doing a parsing by ourselves. This is the propose of
    *  "parseRules()".
    * */
    public Map<String, Map<String, org.sonar.api.rules.Rule>> parseRules(){

        for(String language : languages){

            String fileDir = "coverity-" + language + ".xml";
            InputStream in = getClass().getResourceAsStream(fileDir);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = null;
            Document doc = null;
            try {
                dBuilder = dbFactory.newDocumentBuilder();
                doc = dBuilder.parse(in);
            } catch (ParserConfigurationException e) {
                LOG.error("Error parsing rules." + e.getCause());
            }
             catch (SAXException e) {
                 LOG.error("Error parsing rules." + e.getCause());
             } catch (IOException e) {
                LOG.error("Error parsing rules." + e.getCause());
            }
            doc.getDocumentElement().normalize();

            NodeList nodes = doc.getElementsByTagName("rule");

            if(language.equals("java")){
                javaNodes = nodes;
                mapOfNodeLists.put("java", javaNodes);
            } else if (language.equals("cpp")){
                cppNodes = nodes;
                mapOfNodeLists.put("cpp", cppNodes);
            } else if (language.equals("cs")){
                csNodes = nodes;
                mapOfNodeLists.put("cs", csNodes);
            }

            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);

                String key = "";
                String name = "";
                String severity = "";
                String description = "";

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    key = getValue("key", element);
                    name = getValue("name", element);
                    severity = getValue("severity", element);
                    description = getValue("description", element);
                }

                org.sonar.api.rules.Rule covRule = org.sonar.api.rules.Rule.create("coverity-" + language, key);
                covRule.setName(name);
                covRule.setLanguage(language);
                covRule.setDescription(description);
                covRule.setSeverity(RulePriority.valueOf(severity));

                mapOfRuleMaps.get(language).put(key, covRule);
                if(language.equals("cpp")){
                    org.sonar.api.rules.Rule covRuleCPlusPlus = org.sonar.api.rules.Rule.create("coverity-" + "c++", key);
                    covRuleCPlusPlus.setName(name);
                    covRuleCPlusPlus.setLanguage("c++");
                    covRuleCPlusPlus.setDescription(description);
                    covRuleCPlusPlus.setSeverity(RulePriority.valueOf(severity));
                    mapOfRuleMaps.get("c++").put(key, covRuleCPlusPlus);

                    org.sonar.api.rules.Rule covRuleC = org.sonar.api.rules.Rule.create("coverity-" + "c", key);
                    covRuleC.setName(name);
                    covRuleC.setLanguage("c");
                    covRuleC.setDescription(description);
                    covRuleC.setSeverity(RulePriority.valueOf(severity));
                    mapOfRuleMaps.get("c").put(key, covRuleC);
                }
            }
        }

        return mapOfRuleMaps;
    }

    private static String getValue(String tag, Element element) {
        NodeList nodes = element.getElementsByTagName(tag).item(0).getChildNodes();
        Node node = (Node) nodes.item(0);
        return node.getNodeValue();
    }

    @Override
    public void define(Context context) {
        parseRules();

        /* These extra repositories are added in order to support the community version of c++ plugin and the licensed
        *  version (called cpp). Also we create a "c profile", although rules for c, cpp and c++ are the same.
        */
        List<String> otherLanguages = new ArrayList<String>();
        otherLanguages.add("c++");
        otherLanguages.add("c");

        for(String language : otherLanguages){
            NewRepository repository = context.createRepository(CoverityPlugin.REPOSITORY_KEY + "-" + language, language).setName(language + "-repository");
            String fileDir = "coverity-cpp.xml";
            InputStream in = getClass().getResourceAsStream(fileDir);
            xmlLoader.load(repository, in, "UTF-8");
            repository.done();
        }

        for(String language : languages){
            NewRepository repository = context.createRepository(CoverityPlugin.REPOSITORY_KEY + "-" + language, language).setName(language + "-repository");
            String fileDir = "coverity-" + language + ".xml";
            InputStream in = getClass().getResourceAsStream(fileDir);
            xmlLoader.load(repository, in, "UTF-8");
            repository.done();
        }

    }

    class InternalRule{
        String key;
        String name;
        String severity;
        String description;
        String language;

        InternalRule(String key, String name, String severity, String description, String language){
            this.key = key;
            this.name = name;
            this.severity = severity;
            this.description = description;
            this.language = language;
        }

    }
}


