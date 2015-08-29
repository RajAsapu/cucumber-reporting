package net.masterthought.cucumber.json;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.googlecode.totallylazy.Sequence;
import com.googlecode.totallylazy.Sequences;

import net.masterthought.cucumber.ReportBuilder;
import net.masterthought.cucumber.json.support.ScenarioResults;
import net.masterthought.cucumber.json.support.StepResults;
import net.masterthought.cucumber.util.Status;
import net.masterthought.cucumber.util.StatusCounter;
import net.masterthought.cucumber.util.Util;

public class Feature {

    private final String id = null;
    private final String name = null;
    private final String uri = null;
    private final String description = null;
    private final String keyword = null;
    private final Element[] elements = new Element[0];
    private final Tag[] tags = new Tag[0];

    private StepResults stepResults;
    private ScenarioResults scenarioResults;

    private String jsonFile = "";

    public String getDeviceName() {
        String name = "";
        String[] splitedJsonFile = jsonFile.split("_");
        if (splitedJsonFile.length > 1)
            name = splitedJsonFile[0].substring(0, splitedJsonFile[0].length() - 1);
      return name;
    }

    public void setJsonFile(String json){
        this.jsonFile = json;
    }

    public Sequence<Element> getElements() {
        return Sequences.sequence(elements).realise();
    }

    public String getFileName() {
        List<String> matches = new ArrayList<String>();
        for (String line : Splitter.onPattern("/|\\\\").split(uri)) {
            String modified = line.replaceAll("\\)|\\(", "");
            modified = StringUtils.deleteWhitespace(modified).trim();
            matches.add(modified);
        }

        List<String> lastElement = matches.subList(1, matches.size());

        matches = lastElement.isEmpty() ? matches : lastElement;
        String fileName = Joiner.on("-").join(matches); 

        //If we spect to have parallel executions, we add 
        if (ReportBuilder.isParallel() && !jsonFile.isEmpty()) {
            String[] splitedJsonFile = jsonFile.split("_");
            if (splitedJsonFile.length > 1)
                fileName = fileName + "-" + getDeviceName();
        }
        fileName = fileName + ".html";
        return fileName;
    }

    public String getUri(){
        return this.uri;
    }

    public boolean hasTags() {
        return Util.arrayNotEmpty(tags);
    }

    public boolean hasScenarios() {
        return !getElements().isEmpty();
    }

    public Sequence<Tag> getTags() {
        return Sequences.sequence(tags).realise();
    }

    public String getTagsList() {
        return Util.tagsToHtml(tags);
    }

    public Status getStatus() {
        Sequence<Status> results = getElements().map(Element.Functions.status());
        return results.contains(Status.FAILED) ? Status.FAILED : Status.PASSED;
    }

    public String getName() {
        return Util.itemExists(name) ? getStatus().toHtmlClass()
                + "<div class=\"feature-line\"><span class=\"feature-keyword\">" + keyword + ":</span> " + name
                + "</div></div>" : "";
    }

    public String getRawName() {
        return Util.itemExists(name) ? StringEscapeUtils.escapeHtml(name) : "";
    }

    public String getRawStatus() {
        return getStatus().toString().toLowerCase();
    }

    public String getDescription() {
        String result = "";
        if (Util.itemExists(description)) {
            String content = description.replaceFirst("As an", "<span class=\"feature-role\">As an</span>");
            content = content.replaceFirst("I want to", "<span class=\"feature-action\">I want to</span>");
            content = content.replaceFirst("So that", "<span class=\"feature-value\">So that</span>");
            content = content.replaceAll("\n", "<br/>");
            result = "<div class=\"feature-description\">" + content + "</div>";
        }
        return result;
    }

    public int getNumberOfScenarios() {
        int result = 0;
        if (elements != null) {
            List<Element> elementList = new ArrayList<Element>();
            for (Element element : elements) {
                if (!element.isBackground()) {
                    elementList.add(element);
                }
            }
            result = elementList.size();
        }
        return result;
    }

    public int getNumberOfSteps() {
        return stepResults.getNumberOfSteps();
    }

    public int getNumberOfPasses() {
        return stepResults.getNumberOfPasses();
    }

    public int getNumberOfFailures() {
        return stepResults.getNumberOfFailures();
    }

    public int getNumberOfPending() {
        return stepResults.getNumberOfPending();
    }

    public int getNumberOfSkipped() {
        return stepResults.getNumberOfSkipped();
    }

    public int getNumberOfMissing() {
        return stepResults.getNumberOfMissing();
    }

    public int getNumberOfUndefined() {
        return stepResults.getNumberOfUndefined();
    }

    public String getDurationOfSteps() {
        return stepResults.getTotalDurationAsString();
    }

    public int getNumberOfScenariosPassed() {
        return scenarioResults.getNumberOfScenariosPassed();
    }

    public int getNumberOfScenariosFailed() {
        return scenarioResults.getNumberOfScenariosFailed();
    }

    public void processSteps() {
        List<Step> allSteps = new ArrayList<Step>();
        StatusCounter stepsCounter = new StatusCounter();
        List<Element> passedScenarios = new ArrayList<>();
        List<Element> failedScenarios = new ArrayList<>();
        long totalDuration = 0L;

        if (elements != null) {
            for (Element element : elements) {
                calculateScenarioStats(passedScenarios, failedScenarios, element);
                if (element.hasSteps()) {
                    for (Step step : element.getSteps()) {
                        allSteps.add(step);
                        stepsCounter.incrementFor(step.getStatus());
                        totalDuration += step.getDuration();
                    }
                }
            }
        }
        scenarioResults = new ScenarioResults(passedScenarios, failedScenarios);
        stepResults = new StepResults(allSteps, stepsCounter, totalDuration);
    }

    private void calculateScenarioStats(List<Element> passedScenarios, List<Element> failedScenarios, Element element) {
        if (!element.isBackground()) {
            if (element.getStatus() == Status.PASSED) {
                passedScenarios.add(element);
            } else if (element.getStatus() == Status.FAILED) {
                failedScenarios.add(element);
            }
        }
    }


}
