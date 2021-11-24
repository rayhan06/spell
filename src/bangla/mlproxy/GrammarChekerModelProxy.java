package bangla.mlproxy;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import bangla.ErrorsInBanglaLanguage;
import bangla.diffcalculations.DiffText;
import config.GlobalConfigConstants;
import config.GlobalConfigDTO;
import config.GlobalConfigurationRepository;

import bangla.dto.*;

public class GrammarChekerModelProxy {

	public Map<Integer, WordDTO> getMap(String content, int startKey, int startIndex) throws ParseException {
		Map<Integer, WordDTO> result = new HashMap<Integer, WordDTO>();

		GlobalConfigDTO urlConfigDto = GlobalConfigurationRepository.getGlobalConfigDTOByID(GlobalConfigConstants.CALL_TO_ML); // "http://119.148.4.20:8046/data_annotation_tools/sentence/100";

		if (urlConfigDto == null || urlConfigDto.value == "No") return result;

		urlConfigDto = GlobalConfigurationRepository.getGlobalConfigDTOByID(GlobalConfigConstants.URL_FOR_ML);

		if (urlConfigDto == null || urlConfigDto.value == null || urlConfigDto.value.equals("")) return result;
		return getMap(content, startKey, startIndex, urlConfigDto.value + "grammar_checker.v2");
	}

	private Map<Integer, WordDTO> getMap(String content, int startKey, int startIndex, String urlString) throws ParseException {
		Map<Integer, WordDTO> result = new HashMap<Integer, WordDTO>();

		HttpURLConnection conn = null;
		DataOutputStream os = null;

		try {

			URL url = new URL(urlString);

			String input = "{\"content\":\"" + JSONObject.escape(content) + "\"}";

			byte[] postData = input.getBytes(StandardCharsets.UTF_8);
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("charset", "utf-8");
			conn.setRequestProperty("Content-Length", Integer.toString(input.length()));
			os = new DataOutputStream(conn.getOutputStream());
			os.write(postData);
			os.flush();

			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
						+ conn.getResponseCode());
			}


			StringBuffer resultString = new StringBuffer();

			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			String output;
			while ((output = br.readLine()) != null) {
				resultString.append(output);
			}
			conn.disconnect();

			JSONObject object = (JSONObject) (new JSONParser().parse(resultString.toString()));

			//SpellCheckingDto object = SpellCheckingDto.createObject(resultString.toString());
			int key = startKey;
			for (Object p : object.keySet()) {
				List<WordDTO> dtos = new DiffText().getDifferentDtos(content, (String) object.get(p), startIndex);
				for (WordDTO item : dtos) {
					item.errorCode = prepareErrorCode(Integer.parseInt(p.toString()));
					result.put(key++, item);
				}
			}

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}


		return result;
	}

	private static final int NO_SPACE_BETWEEN_WORDS = 1;
	private static final int PUNCTUATION_ERROR = 2;
	private static final int PREPOSITION_RELATED_ERROR = 4;
	private static final int SOMOCCARITO_ERROR = 8;
	private static final int PROKRITI_PROTYO_JONITO_ERROR = 16;


	private int prepareErrorCode(int mlErrorCode) {
		if (mlErrorCode == 0) return 0;
		int errorCode = 1;

		if ((mlErrorCode & NO_SPACE_BETWEEN_WORDS) > 0) errorCode |= ErrorsInBanglaLanguage.NO_SPACE_PROBLEM;
		if ((mlErrorCode & PUNCTUATION_ERROR) > 0) errorCode |= ErrorsInBanglaLanguage.PUNCTUATION_ERROR;
		if ((mlErrorCode & PREPOSITION_RELATED_ERROR) > 0)
			errorCode |= ErrorsInBanglaLanguage.PREPOSITION_RELATED_ERROR;
		if ((mlErrorCode & SOMOCCARITO_ERROR) > 0) errorCode |= ErrorsInBanglaLanguage.IRRELEVANT_WORD_USAGE_ERROR;
		if ((mlErrorCode & PROKRITI_PROTYO_JONITO_ERROR) > 0) errorCode |= ErrorsInBanglaLanguage.NIRDESHOK_ERROR;

		return errorCode;
	}

	public Hashtable<String, Hashtable<Integer, List<String>>> getUnknownWordsInformation(SentenceDTO sentenceDTO,
	                                                                                      List<WordDTO> unknownWords) {
		Hashtable<String, Hashtable<Integer, List<String>>> result = new Hashtable<String, Hashtable<Integer, List<String>>>();

		GlobalConfigDTO urlConfigDto = GlobalConfigurationRepository.getGlobalConfigDTOByID(GlobalConfigConstants.CALL_TO_ML); // "http://119.148.4.20:8046/data_annotation_tools/sentence/100";

		if (urlConfigDto == null || urlConfigDto.value == "No") return result;

		urlConfigDto = GlobalConfigurationRepository.getGlobalConfigDTOByID(GlobalConfigConstants.URL_FOR_ML);

		if (urlConfigDto == null || urlConfigDto.value == null || urlConfigDto.value.equals("")) return result;


		String[] strings = new String[unknownWords.size()];
		for (int index = 0; index < strings.length; index++) {
			strings[index] = unknownWords.get(index).word;
		}
		HttpURLConnection conn = null;
		DataOutputStream os = null;

		try {

			URL url = new URL(urlConfigDto.value + "unknown_word_suggestion");

			String input = "{\"content\":\"" + JSONObject.escape(sentenceDTO.sentence) + "\", \"unknown_words\":\"" + JSONObject.escape(String.join(",", strings)) + "\"}";
			System.out.println(input);
			byte[] postData = input.getBytes(StandardCharsets.UTF_8);
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("charset", "utf-8");
			conn.setRequestProperty("Content-Length", Integer.toString(input.length()));
			os = new DataOutputStream(conn.getOutputStream());
			os.write(postData);
			os.flush();

			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
						+ conn.getResponseCode());
			}


			StringBuffer resultString = new StringBuffer();

			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			String output;
			while ((output = br.readLine()) != null) {
				resultString.append(output);
			}
			conn.disconnect();

			JSONObject object = (JSONObject) (new JSONParser().parse(resultString.toString()));

			JSONObject o1 = (JSONObject) object.get("word_sep_pred");
			JSONObject o2 = (JSONObject) object.get("lang_model_pred");

			for (WordDTO word : unknownWords) {
				if (result.containsKey(word.word)) continue;
				Hashtable<Integer, List<String>> data = new Hashtable<Integer, List<String>>();
				String spacedString = (String) o1.get(word.word);
				if (spacedString != null) {
					List<String> spacedStrings = new ArrayList<String>();
					spacedStrings.add(spacedString);
					data.put(ErrorsInBanglaLanguage.NO_SPACE_PROBLEM, spacedStrings);
				}

				List<String> predictedStrings = new ArrayList<String>();
				JSONArray predictions = (JSONArray) o2.get(word.word);
				if(predictions != null) {
					for (Object item : predictions.toArray()) {
						predictedStrings.add(((String)item));
					}
				}

				Set<String> predictedStringSet = new HashSet<>(predictedStrings);
				if(!predictedStringSet.contains(word.word)) {
					data.put(ErrorsInBanglaLanguage.UNKNOWN_WORD_ERROR, predictedStrings);
				}

				result.put(word.word, data);
			}

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
		return result;
	}

	public Hashtable<Integer, List<String>> getRealWordSuggestions(SentenceDTO sentenceDTO, List<WordDTO> confusingWords, int realWordThreshold) {
		Hashtable<Integer, List<String>> result = new Hashtable<>();

		GlobalConfigDTO urlConfigDto = GlobalConfigurationRepository.getGlobalConfigDTOByID(GlobalConfigConstants.CALL_TO_ML);

		if (urlConfigDto == null || urlConfigDto.value.equalsIgnoreCase("NO")) return result;

		urlConfigDto = GlobalConfigurationRepository.getGlobalConfigDTOByID(GlobalConfigConstants.URL_FOR_ML);

		if (urlConfigDto == null || urlConfigDto.value == null || urlConfigDto.value.equals("")) return result;

		HttpURLConnection conn = null;
		DataOutputStream os = null;

		String[] strings = new String[confusingWords.size()];
		for (int index = 0; index < strings.length; index++) {
			strings[index] = '"' + JSONObject.escape(confusingWords.get(index).word) + '"';
		}

		try {

			URL url = new URL(urlConfigDto.value + "real_word_error_v3");

			String trimmedString = sentenceDTO.sentence.trim();
			String input = "{\"content\":\"" + JSONObject.escape(trimmedString) + "\", \"candidate_words\":[" + String.join(",", strings) +
					"], \"k\":" + realWordThreshold + "}";

			System.out.println(input);
			byte[] postData = input.getBytes(StandardCharsets.UTF_8);
			conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("charset", "utf-8");
			conn.setRequestProperty("Content-Length", Integer.toString(input.length()));
			os = new DataOutputStream(conn.getOutputStream());
			os.write(postData);
			os.flush();

			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
						+ conn.getResponseCode());
			}

			StringBuffer resultString = new StringBuffer();

			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

			String output;
			while ((output = br.readLine()) != null) {
				resultString.append(output);
			}
			conn.disconnect();

			JSONObject object = (JSONObject) (new JSONParser().parse(resultString.toString()));
			System.out.println(object);

			for (WordDTO word : confusingWords) {
				if (result.containsKey(word.word)) continue;
				HashMap<String, Object> startIndexes = (HashMap<String, Object>) object.get(word.word);
				for (String startIndex : startIndexes.keySet()) {
					JSONObject sugg = (JSONObject) startIndexes.get(startIndex);

					long error = (long) sugg.get("error");
					JSONArray suggestions = (JSONArray) sugg.get("suggestions");
					System.out.println(suggestions);

					List<String> predictions = new ArrayList<String>();
					if (error != 0 && suggestions != null) {
						for (Object suggestion : suggestions) {
							predictions.add((String) suggestion);
						}

						Set<String> predictedStringSet = new HashSet<>(predictions);
						if(!predictedStringSet.contains(word.word)) {
							result.put(Integer.parseInt(startIndex), predictions);
						}
					}
				}
			}

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
		return result;
	}
}
