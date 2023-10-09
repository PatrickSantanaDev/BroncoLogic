package com.bsu.truthtables.domain;

import lombok.Data;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class ParsedQuestion {

    //Will remove once I change the implementation in the Parser, but for now they need to stay
    //so they don't break the build
    private ArrayList<Pair<String, String>> resultList;

    //Front end will iterate through each statement in this list in order to grab
    //that statement's resultList
    private ArrayList<ArrayList<Pair<String, String>>> statementList;

    //List of last column values for each problem type
    private String results;

    private Set<String> independentLettersInPremise;
private Set<String> independentLettersInConclusion;


    //Problem types
    private boolean argument = false;
    private boolean consistency = false;
    private boolean equivalence = false;
    private boolean logical = false;

    //Final answer values
    private String finalAnswer = "";

    private Map<String,String> map;

    public Set<String> getIndependentLettersInPremise() {
        return independentLettersInPremise;
    }
    
    public void setIndependentLettersInPremise(Set<String> independentLettersInPremise) {
        this.independentLettersInPremise = independentLettersInPremise;
    }
    
    public Set<String> getIndependentLettersInConclusion() {
        return independentLettersInConclusion;
    }
    
    public void setIndependentLettersInConclusion(Set<String> independentLettersInConclusion) {
        this.independentLettersInConclusion = independentLettersInConclusion;
    }
    
}
