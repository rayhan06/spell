package bangla.grammarchecker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import config.GlobalConfigConstants;
import config.GlobalConfigDTO;
import config.GlobalConfigurationRepository;

public class GrammerCheckerFactory {

	private GrammerCheckerFactory() {
	}

	private static String registeredTypes = null;
	private static Object syncObject = new Object();
	private static Map<String, BanglaGrammarChecker> grammarCheckerMap = new HashMap<>();

	private static BanglaGrammarChecker getGrammerChecker(String checkerType) {
		if (checkerType == null)
			return null;
		if (checkerType.equals(GrammerCheckerTypes.SHADHU_CHOLIT_ERROR.toString()))
			return new ShadhuCholitMixureChecker();
		else if (checkerType.equals(GrammerCheckerTypes.SUBJECT_VERB_AGREEMENT_ERROR.toString()))
			return new SubVerbRelErrorChecker();
		return null;
	}

	public static void initializeRegisteredCheckers() {
		if (registeredTypes != null)
			return;
		synchronized (syncObject) {
			if (registeredTypes != null)
				return;
			GlobalConfigDTO registerType = GlobalConfigurationRepository.getGlobalConfigDTOByID(GlobalConfigConstants.REGISTERED_GRAMMAR_CHECKER_TYPES);
			if (registerType == null || registerType.value == null || registerType.value == "")
				return;
			registeredTypes = registerType.value;
			String[] splittedEnums = registeredTypes.split(",");

			for (int index = 0; index < splittedEnums.length; index++) {
				BanglaGrammarChecker checker = getGrammerChecker(splittedEnums[index]);
				if (checker == null)
					continue;
				grammarCheckerMap.put(splittedEnums[index], checker);
			}
		}
	}

	public static Map<String, BanglaGrammarChecker> getRegisteredCheckers() {
		GlobalConfigDTO registerType = GlobalConfigurationRepository
				.getGlobalConfigDTOByID(GlobalConfigConstants.REGISTERED_GRAMMAR_CHECKER_TYPES);
		if (registerType == null || registerType.value == null || registerType.value == "" || registerType.value == registeredTypes)
			return grammarCheckerMap;

		synchronized (syncObject) {
			registeredTypes = registerType.value;
			String[] splittedEnums = registeredTypes.split(",");
			List<String> items = Arrays.asList(splittedEnums);
			List<String> removedList = new ArrayList<String>();
			for (String key : grammarCheckerMap.keySet()) {
				if (!items.contains(key)) {
					removedList.add(key);
				}
			}
			for (String removedItem : removedList) {
				grammarCheckerMap.remove(removedItem);
			}

			for (int index = 0; index < splittedEnums.length; index++) {
				if (!grammarCheckerMap.containsKey(splittedEnums[index])) {
					BanglaGrammarChecker checker = getGrammerChecker(splittedEnums[index]);
					if (checker != null) {
						grammarCheckerMap.put(splittedEnums[index], checker);
					}
				}
			}

		}


		return grammarCheckerMap;
	}

}
