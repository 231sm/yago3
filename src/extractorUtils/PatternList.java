package extractorUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javatools.administrative.Announce;
import javatools.datatypes.Pair;
import basics.Fact;
import basics.FactCollection;

/**
 * Replaces patterns by strings
 * 
 * @author Fabian M. Suchanek
 *
 */
public class PatternList {

	/** Holds the patterns to apply */
	public final List<Pair<Pattern, String>> patterns=new ArrayList<Pair<Pattern,String>>();

	/** Constructor */
	public PatternList(FactCollection facts, String relation) {
		Announce.doing("Loading patterns of",relation);
		for (Fact fact : facts.get(relation)) {
			patterns.add(new Pair<Pattern, String>(fact.getArgPattern(1), fact.getArgString(2)));
		}
		if(patterns.isEmpty()) {
			Announce.warning("No patterns found!");
		}
		Announce.done();
	}

	/** Replaces all patterns in the string */
	public String transform(String input) {
		for (Pair<Pattern, String> pattern : patterns) {
			input = pattern.first.matcher(input).replaceAll(pattern.second);
			if (input.contains("NIL") && pattern.second.equals("NIL"))
				return (null);
		}
		return (input);
	}
}