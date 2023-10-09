package com.bsu.truthtables.parser;

import com.bsu.truthtables.domain.ParsedQuestion;
import org.javatuples.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class Parser {

    @Value("${operators}")  // loaded from application properties, this is all the known operators: ~^v=><-
    private String operators;
    private String stmt;
    private String original;
    private String chars;
    private String errorMessage;
    @Value("#{${prefilledBox}}")  // {1:'TF',2:'TTTFFTFF',3:'TTTTTFTFTTFFFTTFTFFFTFFF',4:'TTTTTTTFTTFTTTFFTFTTTFTFTFFTTFFFFTTTFTTFFTFTFTFFFFTTFFTFFFFTFFFF', 5:'TTTTTTTTTFTTTFTTTTFFTTFTTTTFTFTTFFTTTFFFTFTTTTFTTFTFTFTTFTFFTFFTTTFFTFTFFFTTFFFFFTTTTFTTTFFTTFTFTTFFFTFTTFTFTFFTFFTFTFFFFFTTTFFTTFFFTFTFFTFFFFFTTFFFTFFFFFTFFFFF'}
    private Map<String, String> prefilledBox;
    private HashMap<String, String> map = null;
    public ArrayList<ArrayList<String>> data;
    private ParsedQuestion parsedQuestion = null;

    public String orderResults(ArrayList<Pair<String, String>> r) {
        int max = 0;
        for (Pair<String, String> p : r) {
            if (p.getValue1().length() > max) {
                max = p.getValue1().length();
            }
        }
        String res = "";
        for (int i = 0; i < max; i++) {
            for (Pair<String, String> p : r) {
                if (p.getValue1().length() != 0) {
                    res += p.getValue1().charAt(i);
                }
            }
        }
        return res;
    }

	// Removes all special/padding characters, returning string containing only the variables (A-Z,a-z)
    public String parseChars(String question) {
        return question.replaceAll("[" + operators + "]", "")  // Remove defined operators
                .replaceAll("\\(", "") // Remove left parentheses
                .replaceAll("\\)", "") // Remove right parentheses
                .replaceAll(" ", "")   // Remove whitespace
                .replaceAll("\\:", "") // Remove colons
                .replaceAll("\\.", "") // Remove periods
                .replaceAll(",", "")   // Remove commas
                .chars()     		   // Convert to int codes.
                .distinct()            // Remove duplicates.
                .mapToObj(c -> String.valueOf((char) c)).collect(Collectors.joining());  // Map as string containing pertinent characters.
    }

	// Wrapper for char -> string.
	private String str(char c) {
		return String.valueOf(c);
	}

    public String getErrorMessage() {
        return errorMessage;
    }
	
	public boolean correctParen(String s) {
		// Replace operators with newline. Users *shouldn't* be able to insert newlines in their input.
		s=s.replaceAll("v","\n").replaceAll("\\^","\n").replaceAll("<->","\n").replaceAll("->","\n");
		LinkedList<Boolean> unboundedOperator = new LinkedList<Boolean>();
		int open=0;
		Boolean b=new Boolean(false);
		for (int i=0; i<s.length(); i++) {
			char c=s.charAt(i);
			if (c=='(') {  // Entering new scope. Push last scope's value.
				unboundedOperator.push(b);
				open++;
				b=new Boolean(false);  // We haven't hit an operator yet this scope.
			}
			else if (c==')') {  // Leaving scope. Retrieve last scope's value.
				if (unboundedOperator.size()<1) {
                    errorMessage = "Invalid Expression. Closing parenthesis should have corresponding opening parenthesis";
					return false;  // If ')' without '(' appearing first...
                }
				b=unboundedOperator.pop();
				open--;
			}
			else if (c=='\n') {  // We hit a middle operator...
				if (b.booleanValue()) { // If this scope already had an operator...
                    errorMessage = "Invalid expression. Add/adjust parentheses to ensure multiple operators don't appear within a single scope.";
					return false;  // Invalid parentheses.
                }
				b=new Boolean(true);  // Else, mark that we've hit an operator this scope.
			}
		}
        
        if (open != 0) {
            errorMessage = "Invalid expression. Opening parenthesis should have corresponding closing parenthesis";
            return false;
        }

		errorMessage = "";  // Reset error message if everything is fine
        return true;
	}
	
	// Test function. Could be moved elsewhere if testing suite made later.
	private void pt(String s, boolean desired) {
		if (correctParen(s)==desired) {
			System.out.println("Test for "+s+" passed.");
		}
		else {
			System.out.println("WARNING! Test for "+s+" failed!");
		}
	}
	
	// Parser tests.
	private void parenTests() {
		pt("avb",true);
		pt("(avb)",true);
		pt("(avb)^(c->d)",true);
		pt("((avb)vc)",true);
		pt("(av(bvc))",true);
		pt("a<->c",true);
		pt("(((a->d)))",true);
		pt("((((avb)vc)vd)ve)",true);
		pt("((a^b)v(a->d))<->e",true);
		pt("(a)v(b)",true);
		
		pt("(avb",false);         // No closing paren.
		pt("avb)",false);	      // No opening paren.
		pt("avbvc",false);        // Ambiguous ordering. (Applies to all others)
		pt("(avb->c)",false);
		pt("dv(avb)vc",false);
		pt("(a^(b^c)->c)",false);
		pt("(a)v(b)^(c)",false);
		pt("((avbvc))",false);
	}

    public ParsedQuestion parseQuestion(String question) {
        parenTests();
        System.out.println("Question:" + question);
        parsedQuestion = new ParsedQuestion();
        map = new LinkedHashMap<>();
        stmt = question.replaceAll(" ", "");
        original = stmt;
        chars = parseChars(question);
        if (!correctParen(question)) {
            System.out.println("ERROR! Invalid parentheses use!");
            return parsedQuestion;
        }
        String values = prefilledBox.get(String.valueOf(chars.length()));
        for (int i = 0; i < values.length(); i++) {
            if (get(str(chars.charAt(i % chars.length()))) == null)
                put(str(chars.charAt(i % chars.length())), String.valueOf(values.charAt(i)));
            else
                put(str(chars.charAt(i % chars.length())), get(str(chars.charAt(i % chars.length()))) + values.charAt(i));
        }
        determineType();
    
        // Extract independent letters from premise and conclusion
        String[] list = original.split(":\\.");
        if (list.length == 2) { // Ensure there's both a premise and a conclusion
            String[] premises = list[0].split(",");
            String conclusion = list[1];
            
            parsedQuestion.setIndependentLettersInPremise(extractIndependentLettersFromPremises(premises));
            parsedQuestion.setIndependentLettersInConclusion(extractIndependentLettersFromConclusion(conclusion));
        }

                System.out.println("Independent Letters in Premise: " + parsedQuestion.getIndependentLettersInPremise());
        System.out.println("Independent Letters in Conclusion: " + parsedQuestion.getIndependentLettersInConclusion());

    
        parsedQuestion.setResultList(getData());
        if (parsedQuestion.isConsistency())
            evalConsistency();
        else if (parsedQuestion.isArgument())
            evalArgument();
        else if (parsedQuestion.isEquivalence())
            evalEquivalence();
        else
            evalLogical();
        unicode();
        return parsedQuestion;
    }

    private Set<String> extractIndependentLettersFromPremises(String[] premises) {
        Set<String> letters = new HashSet<>();
        
        // For premises
        for (String premise : premises) {
            // Split the premise based on logical operators
            String[] tokens = premise.split("[^A-Z]");
            // If there's only one token and it's a single letter, add it
            if (tokens.length == 1 && tokens[0].length() == 1) {
                letters.add(tokens[0]);
            }
        }
        
        return letters;
    }
    
    private Set<String> extractIndependentLettersFromConclusion(String conclusion) {
        Set<String> letters = new HashSet<>();
        
        // For conclusion
        for (char c : conclusion.toCharArray()) {
            if (Character.isUpperCase(c)) {
                letters.add(String.valueOf(c));
            }
        }
        
        return letters;
    }
    
    
    
    
    
    

    public void evalArgument() {
        String[] list = original.split(":.");
        String[] premises = list[0].split(",");
        String conclusion = list[1];
        String validity = "";
        String valid = "valid";
        for (int i=0; i<get(conclusion).length(); i++) {
            boolean allPremisesTrue = true;
            String tmp = "F";
            for (String premise : premises) {
                if (map.get(premise).charAt(i) == 'F') allPremisesTrue = false;
            }
            if (allPremisesTrue && map.get(conclusion).charAt(i) == 'F') {
                tmp = "T";
                valid = "invalid";
            }
            validity += tmp;
        }
        parsedQuestion.setFinalAnswer(valid);
        parsedQuestion.setResults(validity);
    }

    public void evalConsistency() {
        String[] list = original.split(",");
        String consistent = "";
        String cons = "not consistent";
        for (int i = 0; i < map.get(list[0]).length(); i++) {
            String tmp = "T";
            for (String s : list) {
                if (map.get(s).charAt(i) == 'F') {
                    tmp = "F";
                }
            }
            if (tmp.equals("T")) cons = "consistent";
            consistent += tmp;
        }
        parsedQuestion.setResults(consistent);
        parsedQuestion.setFinalAnswer(cons);
    }

    public void evalEquivalence() {
        String[] list = original.split("::");
        String equivalent = "";
        String equiv = "equivalent";
        for (int i = 0; i < map.get(list[0]).length(); i++) {
            String tmp = "F";
            char c1 = map.get(list[0]).charAt(i);
            char c2 = map.get(list[1]).charAt(i);
            if (c1 != c2) {
                equiv = "not equivalent";
                tmp = "T";
            }
            equivalent += tmp;
        }
        parsedQuestion.setResults(equivalent);
        parsedQuestion.setFinalAnswer(equiv);
    }

    public void evalLogical() {
        String values = map.get(original);
        String logical = "";
        boolean taut = true;
        boolean contra = true;
        for (int i = 0; i < values.length(); i++) {
            char c = values.charAt(i);
            String tmp = "F";
            if (c == 'T') {
                contra = false;
            } else {
                tmp = "T";
                taut = false;
            }
            logical += tmp;
        }
        parsedQuestion.setResults(logical);
        String status = "";
        if (taut) status = "tautology";
        else if (contra) status = "contradiction";
        else status = "contingent";
        parsedQuestion.setFinalAnswer(status);
    }

	// Sets the type of question being asked. I don't know what's being returned?
    public String determineType() {
        if (stmt.contains("::")) {
            parsedQuestion.setEquivalence(true);
            stmt = stmt.replaceAll("::", ",");
            while (stmt.contains(",")) {
                stmt();
                map.put(",", "");
            }
            return stmt();
        } else if (stmt.contains(":.")) {
            parsedQuestion.setArgument(true);
            stmt = stmt.replaceAll(":.", ",");
            while (stmt.contains(",")) {
                stmt();
                map.put(",", "");
            }
            return stmt();
        } else if (stmt.contains(",")) {
            parsedQuestion.setConsistency(true);
            while (stmt.contains(",")) {
                stmt();
                map.put(",", "");
            }
            return stmt();
        }
        parsedQuestion.setLogical(true);
        while (stmt.contains(",")) {
            stmt();
            map.put(",", "");
        }
        return stmt();
    }

    public String stmt() {  // I also don't know why this exists?
        return t0();
    }

    public String t0() {
        String obj = t1();
        if (more() && peek() == '<' && doublePeek() == '-' && triplePeek() == '>') {
            eat('<');
            eat('-');
            eat('>');
            String obj2 = t1();
            return ifAndOnlyIf(obj, obj2);
        }
        return obj;
    }

    public String t1() {
        String obj = t2();
        if (more() && peek() == '-' && doublePeek() == '>') {
            eat('-');
            eat('>');
            String obj2 = t2();
            return ifThen(obj, obj2);
        }
        return obj;
    }

    public String t2() {
        String obj = t3();
        while (more() && peek() == 'v') {
            eat('v');
            String obj2 = t3();
            obj = or(obj, obj2);
        }
        return obj;
    }

    public String t3() {
        String obj = t4();
        while (more() && peek() == '^') {
            eat('^');
            String obj2 = t4();
            obj = and(obj, obj2);
        }
        return obj;
    }

    public String t4() {  // I have no idea why this exists?
        return t5();
    }

    public String t5() {
        int numOfNots = 0;
        while (peek() == '~') {
            eat('~');
            numOfNots++;
        }
        if (numOfNots != 0 && numOfNots % 2 == 1) {          //idk what the char is for not so I am using ! temporarily
            String obj = t6();
            return not(obj);
        }
        return t6();
    }

    public String t6() {
        if (more() && peek() == '(') {
            eat('(');
            String s = stmt();  // t0 calls t1, t1 calls t2, etc., so essentially a recursive call.
            while (peek() == ')')
                eat(')');
            return paren(s);
        }
        return t7();
    }

    public String t7() {
        return str(next());
    }

    public String paren(String o1) {
        String name = "(" + o1 + ")";
        put(name, get(o1));
        map.remove(o1);
        if (stmt.length() == 0)
            return name;
        char op = next();
        if (op == ',')
			return name;
        String o2 = stmt();
        String retval = "";
        if (op == '^') {
            retval = (String) and(name, o2);
            name += "^" + o2;
        }
        if (op == 'v') {
            retval = (String) or(name, o2);
            name += "v" + o2;
        }
        else if (op == '-') {
            retval = (String) ifThen(name, o2);
            name += "->" + o2;
        }
        if (op == '<') {
            retval = (String) ifAndOnlyIf(name, o2);
            name += "<->" + o2;
        }
        put(name, get(retval));
        return name;
    }

    public String not(String o1) {
        String retVal = "";
        String name = "~" + o1;
        for (int i = 0; i < get(o1).length(); i++) {
            if (get(o1).charAt(i) == 'T') {
                retVal += "F";
            } else {
                retVal += "T";
            }
        }
        map.put(name, retVal);
        return name;
    }

    public String and(String o1, String o2) {
        String retVal = "";
        String name = o1 + "^" + o2;
        for (int i = 0; i < get(o1).length(); i++) {
            if (get(o1).charAt(i) == 'T' && get(o2).charAt(i) == 'T') {
                retVal += "T";
            } else {
                retVal += "F";
            }
        }
        map.put(name, retVal);
        return name;
    }

    public String or(String o1, String o2) {
        String retVal = "";
        String name = o1 + "v" + o2;
        for (int i = 0; i < get(o1).length(); i++) {
            if (get(o1).charAt(i) == 'T' || get(o2).charAt(i) == 'T') {
                retVal += "T";
            } else {
                retVal += "F";
            }
        }
        map.put(name, retVal);
        return name;
    }

    public String ifThen(String o1, String o2) {
        String name = "" + o1;
        put(name, get(o1));
        String retVal = "";
        for (int i = 0; i < get(name).length(); i++) {
            retVal += get(name).charAt(i) == 'T' && get(o2).charAt(i) == 'F' ? "F" : "T";
        }
        name = name + "->" + o2;
        put(name, retVal);
        return name;
    }

    public String ifAndOnlyIf(String o1, String o2) {
        String name = "" + o1;
        put(name, get(o1));
        String retVal = "";
        for (int i = 0; i < get(name).length(); i++) {
            retVal += get(name).charAt(i) == get(o2).charAt(i) ? "T" : "F";
        }
        name = name + "<->" + o2;
        put(name, retVal);
        return name;
    }

    //helper methods for parser
    private char peek() {
        if (stmt.length() == 0) {
            return '0';
        }
        return stmt.charAt(0);
    }

    private char doublePeek() {
        if (stmt.length() < 1) {
            return '0';             //return 0 since it wont ever equal a literal, else it might throw error
        }
        return stmt.charAt(1);
    }

    private char triplePeek() {
        if (stmt.length() < 2) {
            return '0';                 //return 0 since it wont ever equal a literal, else it might throw error
        }
        return stmt.charAt(2);
    }

    private void eat(char c) {
        if (peek() == c)
            this.stmt = this.stmt.substring(1);
        else
            throw new RuntimeException("Expected: " + c + "; got: " + peek());
    }

	// Returns next char in stmt.
    private char next() {
        char c = peek();
        eat(c);
        return c;
    }

	// Determines if more characters to parse in stmt.
    private boolean more() {
        return stmt.length() > 0;
    }

    public String get(String o) {
        if (o instanceof String) {
            return map.get(o);
        } else {
            return map.get(String.valueOf(o));
        }
    }

    public String put(String o, String val) {
        if (o instanceof String) {
            return map.put(o.toString(), val);
        } else {
            return map.put(String.valueOf(o), val);
        }
    }

    public ArrayList<Pair<String, String>> getData() {
        ArrayList<String> ops = new ArrayList<>();
        ArrayList<String> values = new ArrayList<>();
        ArrayList<String> keys = new ArrayList<>();
        int len = 1;
        for (String key : map.keySet()) {
            if (key.length() > 1) {
                keys.add(key);
                if (key.length() > len) {
                    len = key.length();
                }
            }
        }
        keys.sort(new KeyComparator(original));
        for (String key : keys) {
            String val = map.get(key);
            values.add(val);
        }
        for (int i = 0; i < original.length(); i++) {
            char c = original.charAt(i);
            if (c == '^' || c == 'v' || c == '-' || c == '~') {
                String op = "" + c;
                ops.add(op);
            }
        }
        ArrayList<Pair<String, String>> ret = new ArrayList<>();
        int count = 0;
        for (int i = 0; i < original.length(); i++) {
            String s = "" + original.charAt(i);
            if (parsedQuestion.getIndependentLettersInPremise().contains(s) || parsedQuestion.getIndependentLettersInConclusion().contains(s)) {
                ret.add(new Pair<>(s, " "));  // Using a space as a placeholder
            } else if (count < ops.size() && s.equals(ops.get(count))) {
                ret.add(new Pair<>(s, values.get(count)));
                count++;
            } else {
                ret.add(new Pair<>(s, ""));
            }
        }
        return ret;
    }

    public void unicode() {
        ArrayList<Pair<String, String>> results = parsedQuestion.getResultList();
        int size = results.size();
        for (int i = 0; i < size; i++) {
            Pair<String,String> p1 = results.get(i);
            if (p1.getValue0().equals("~")) {
                String value = p1.getValue1();
                Pair<String,String> newPair = new Pair<>(new String(Character.toChars(172)),value);
                results.set(i, newPair);
            }
            else if (p1.getValue0().equals("-")) {
                String value = p1.getValue1();
                Pair<String,String> newPair = new Pair<>(new String(Character.toChars(8594)),value);
                results.set(i, newPair);
                results.remove(i+1);
                size--;
            }
            else if (p1.getValue0().equals("<")) {
                String value = results.get(i+1).getValue1();
                Pair<String,String> newPair = new Pair<>(new String(Character.toChars(8596)),value);
                results.set(i, newPair);
                results.remove(i+2);
                results.remove(i);
                size-=2;
            }
            else if (p1.getValue0().equals(":")) {
                if (results.get(i+1).getValue0().equals(".")) {
                    Pair<String,String> newPair = new Pair<>(new String(Character.toChars(8756)),"");
                    results.set(i, newPair);
                }
                else {
                    Pair<String,String> newPair = new Pair<>(new String(Character.toChars(8759)),"");
                    results.set(i, newPair);
                }
                results.remove(i+1);
                size--;
            }
        }
    }

    public class KeyComparator implements Comparator<String> {
        public String question;

        public KeyComparator(String question) {
            this.question = question;
        }

        @Override
        public int compare(String o1, String o2) {
            int pos1 = findPos(o1, this.question);
            int pos2 = findPos(o2, this.question);
            if (pos1 < pos2) {
                return -1;
            } else if (pos1 > pos2) {
                return 1;
            }
            int len1 = o1.length();
            int len2 = o2.length();
            if (len1 < len2) {
                return -1;
            }
            return 1;
        }

        public int findPos(String s, String question) {
            int pos = 0;
            boolean found = false;
            for (; pos < question.length(); pos++) {
                for (int i = 0; i < s.length(); i++) {
                    if (!(s.charAt(i) == question.charAt(pos + i))) {
                        break;
                    }
                    if (i == s.length() - 1) {
                        found = true;
                    }
                }
                if (found) break;
            }
            int parenCheck = 0;
            while (s.charAt(parenCheck) == '(') {
                parenCheck++;
                pos++;
            }
            return pos;
        }
    }
}
