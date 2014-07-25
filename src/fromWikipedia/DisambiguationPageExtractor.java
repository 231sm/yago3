package fromWikipedia;

import java.io.BufferedReader;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javatools.administrative.Announce;
import javatools.datatypes.FinalSet;
import javatools.filehandlers.FileLines;
import javatools.util.FileUtils;
import utils.FactCollection;
import utils.FactTemplateExtractor;
import utils.MultilingualTheme;
import utils.Theme;
import basics.Fact;
import extractors.MultilingualWikipediaExtractor;
import followUp.EntityTranslator;
import followUp.FollowUpExtractor;
import followUp.Redirector;
import followUp.TypeChecker;
import fromOtherSources.PatternHardExtractor;

/**
 * Extracts means facts from Wikipedia disambiguation pages
 * 
 * @author Johannes Hoffart
 * 
 */
public class DisambiguationPageExtractor extends MultilingualWikipediaExtractor {

	@Override
	public Set<Theme> input() {
		return new HashSet<Theme>(
				Arrays.asList(PatternHardExtractor.DISAMBIGUATIONTEMPLATES, PatternHardExtractor.LANGUAGECODEMAPPING));
	}

  @Override
  public Set<FollowUpExtractor> followUp() {
    Set<FollowUpExtractor> result = new HashSet<FollowUpExtractor>();
    result.add(new Redirector(DIRTYDISAMBIGUATIONMEANSFACTS.inLanguage(language), REDIRECTEDDISAMBIGUATIONMEANSFACTS.inLanguage(language), this));
    if (!isEnglish()) {
      result.add(new EntityTranslator(REDIRECTEDDISAMBIGUATIONMEANSFACTS.inLanguage(language), TRANSLATEDREDIRECTEDDISAMBIGUATIONMEANSFACTS
          .inLanguage(language), this));
      result.add(new TypeChecker(TRANSLATEDREDIRECTEDDISAMBIGUATIONMEANSFACTS.inLanguage(language), DISAMBIGUATIONMEANSFACTS.inLanguage(language),
          this));
    } else {
      result.add(new TypeChecker(REDIRECTEDDISAMBIGUATIONMEANSFACTS.inLanguage(language), DISAMBIGUATIONMEANSFACTS.inLanguage(language), this));
    }
    return result;
  }

	/** Means facts from disambiguation pages */
	public static final MultilingualTheme DIRTYDISAMBIGUATIONMEANSFACTS = new MultilingualTheme(
			"disambiguationMeansFactsDirty",
			"Means facts from disambiguation pages - needs redirecting and translation, typechecking");

	/** Means facts from disambiguation pages */
	public static final MultilingualTheme REDIRECTEDDISAMBIGUATIONMEANSFACTS = new MultilingualTheme(
			"disambiguationMeansFactsRedirected",
			"Means facts from disambiguation pages - needs translation and typechecking");

	 /** Means facts from disambiguation pages */
  public static final MultilingualTheme TRANSLATEDREDIRECTEDDISAMBIGUATIONMEANSFACTS = new MultilingualTheme(
      "disambiguationMeansFactsTranslated",
      "Means facts from disambiguation pages - needs translation and typechecking");
  
  
	/** Means facts from disambiguation pages */
	public static final MultilingualTheme DISAMBIGUATIONMEANSFACTS = new MultilingualTheme(
			"disambiguationMeansFacts", "Means facts from disambiguation pages");

	@Override
	public Set<Theme> output() {
		return new FinalSet<Theme>(DIRTYDISAMBIGUATIONMEANSFACTS.inLanguage(language));
	}

	@Override
	public void extract() throws Exception {
		// Extract the information
		Announce.doing("Extracting disambiguation means");

		BufferedReader in = FileUtils.getBufferedUTF8Reader(wikipedia);

		FactCollection disambiguationPatternCollection = PatternHardExtractor.DISAMBIGUATIONTEMPLATES
				.factCollection();
		FactTemplateExtractor disambiguationPatterns = new FactTemplateExtractor(
				disambiguationPatternCollection, "<_disambiguationPattern>");
		Set<String> templates = disambiguationTemplates(disambiguationPatternCollection);

		String titleEntity = null;
		String page = null;

		while (true) {
			switch (FileLines.findIgnoreCase(in, "<title>")) {
			case -1:
				Announce.done();
				in.close();
				return;
			case 0:
				titleEntity = FileLines.readToBoundary(in, "</title>");
				titleEntity = cleanDisambiguationEntity(titleEntity);
				page = FileLines.readBetween(in, "<text", "</text>");

				if (titleEntity == null || page == null)
					continue;

				if (isDisambiguationPage(page, templates)) {
					for (Fact fact : disambiguationPatterns.extract(page,
							titleEntity, language)) {
						if (fact != null)
							DIRTYDISAMBIGUATIONMEANSFACTS.inLanguage(language).write(fact);
					}
				}
			}
		}
	}

	protected static String cleanDisambiguationEntity(String titleEntity) {
		if (titleEntity.indexOf("(disambiguation)") > -1) {
			titleEntity = titleEntity.substring(0,
					titleEntity.indexOf("(disambiguation)")).trim();
		} else if (titleEntity.indexOf("(توضيح)") > -1) {//for Arabic
		  titleEntity = titleEntity.substring(0,
          titleEntity.indexOf("(توضيح)")).trim();
		}
		return titleEntity;
	}

	/** Returns the set of disambiguation templates */
	public static Set<String> disambiguationTemplates(
			FactCollection disambiguationTemplates) {
		return (disambiguationTemplates
				.seekStringsOfType("<_yagoDisambiguationTemplate>"));
	}

	private boolean isDisambiguationPage(String page, Set<String> templates) {
		for (String templName : templates) {
			if (page.contains(templName)
					|| page.contains(templName.toLowerCase())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Needs Wikipedia as input
	 * 
	 * @param wikipedia
	 *            Wikipedia XML dump
	 */
	public DisambiguationPageExtractor(String lang, File wikipedia) {
		super(lang, wikipedia);
	}

	public static void main(String[] args) throws Exception {
		String s = "Regular Title";
		String correct = "Regular Title";
		s = DisambiguationPageExtractor.cleanDisambiguationEntity(s);
		if (!s.equals(correct)) {
			System.out.println("Expected: " + correct + ". Value: " + s);
		}

		s = "Regular Title (disambiguation)";
		s = DisambiguationPageExtractor.cleanDisambiguationEntity(s);
		if (!s.equals(correct)) {
			System.out.println("Expected: " + correct + ". Value: " + s);
		}

		s = "Regular Title (disambiguation). ";
		s = DisambiguationPageExtractor.cleanDisambiguationEntity(s);
		if (!s.equals(correct)) {
			System.out.println("Expected: " + correct + ". Value: " + s);
		}

		System.out.println("Done.");
	}

}
