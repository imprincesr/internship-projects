package com.ninja.BankStAnalysis.core.ananomyzer.core;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import net.minidev.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class is used to flatten the json data. This will only select the values
 * specified by jsonPaths.
 * 
 * @author Vijay
 * @since 03/24/18
 */
public final class JsonFlattener {
	private static final Logger logger = LoggerFactory.getLogger(JsonFlattener.class);

	private final String jsonName;
	private final List<String> jsonPaths;
	private char separator = ',';
	private static final Map<String, Map<String, Set<String>>> jsonPathMapCache = new ConcurrentHashMap<>();

	/**
	 * @param jsonName  name of the json which is used to cache map structure
	 *                  created using json paths.
	 * @param jsonPaths list of json paths. @see
	 *                  <a href="https://github.com/json-path/JsonPath">this</a> for
	 *                  json path.
	 */
	public JsonFlattener(String jsonName, List<String> jsonPaths) {

		requireNonNull(jsonPaths, "json path list should not be null");
		checkArgument(!jsonPaths.isEmpty(), "json path list should not be empty");
		this.jsonName = jsonName;
		this.jsonPaths = jsonPaths;
	}

	public JsonFlattener(String jsonName, List<String> jsonPaths, char separator) {

		this(jsonName, jsonPaths);
		this.separator = separator;
	}

	/**
	 * Flattens json passed as argument. It will have values specified by
	 * {@link JsonFlattener#jsonPaths}. {@link JsonFlattener#separator} will be used
	 * to separate the column values.
	 *
	 * @param json input json that needs to be flattened.
	 * @return list of raw values.
	 */
	public List<String> flatten(String json) {

		if (json == null || json.trim().isEmpty()) {
			// return empty list
			return new ArrayList<>(0);
		}
		Map<String, Set<String>> objectMap = toJsonPathMap();
		Object document = Configuration.defaultConfiguration().jsonProvider().parse(json);
		List<JSONRecord> finalRecords = new ArrayList<>();
		if (document instanceof JSONArray) {
			JSONArray array = (JSONArray) document;
			array.forEach((eachDocument) -> {
				Optional<List<JSONRecord>> records = flattenObject(eachDocument, objectMap);
				if (records.isPresent()) {
					finalRecords.addAll(records.get());
				}
			});
		} else {
			Optional<List<JSONRecord>> records = flattenObject(document, objectMap);
			if (records.isPresent()) {
				finalRecords.addAll(records.get());
			}
		}
		return finalRecords.stream().map((record -> record.toString(separator))).collect(Collectors.toList());
	}

	/**
	 * Flattens json passed as argument. It will have values specified by
	 * {@link JsonFlattener#jsonPaths}. {@link JsonFlattener#separator} will be used
	 * to separate the column values.
	 *
	 * @param json input json that needs to be flattened.
	 * @return list of raw values.
	 */
	public List<JSONRecord> flattenv2(String json) {

		if (json == null || json.trim().isEmpty()) {
			// return empty list
			return new ArrayList<>(0);
		}
		Map<String, Set<String>> objectMap = toJsonPathMap();
		Object document = Configuration.defaultConfiguration().jsonProvider().parse(json);
		List<JSONRecord> finalRecords = new ArrayList<>();
		if (document instanceof JSONArray) {
			JSONArray array = (JSONArray) document;
			array.forEach((eachDocument) -> {
				Optional<List<JSONRecord>> records = flattenObject(eachDocument, objectMap);
				if (records.isPresent()) {
					finalRecords.addAll(records.get());
				}
			});
		} else {
			Optional<List<JSONRecord>> records = flattenObject(document, objectMap);
			if (records.isPresent()) {
				finalRecords.addAll(records.get());
			}
		}

		return finalRecords;
	}

	private Optional<List<JSONRecord>> flattenObject(Object document, Map<String, Set<String>> objectMap) {

		TreeNode rootNode = new TreeNode(document, "$", TreeNode.NodeType.OBJECT, "$");
		generateTree(objectMap, rootNode);
		return Optional.ofNullable(createRecords(rootNode));
	}

	/**
	 * Creates map structure which will have key as the json path element and value
	 * as set of elements which are under key according to json paths. e.g for $.Id
	 * an $.Name will have $ as the key and set of {Id,Name} as the value.
	 */
	private Map<String, Set<String>> toJsonPathMap() {

		logger.debug("Creating json path map for jsonName : %s", jsonName);
		Map<String, Set<String>> jsonPathMap = jsonPathMapCache.get(jsonName);

		if (jsonPathMap != null) {
			return jsonPathMap;
		} else {
			jsonPathMap = new HashMap<>();
			jsonPathMap.put("$", new LinkedHashSet<>());
			for (String path : jsonPaths) {
				String[] components = path.split("\\.");
				String parentComponent = "$";
				for (int i = 1; i < components.length; i++) {
					if (!jsonPathMap.containsKey(parentComponent)) {
						Set<String> childSet = new LinkedHashSet<>();
						childSet.add(components[i]);
						jsonPathMap.put(parentComponent, childSet);
					} else {
						jsonPathMap.get(parentComponent).add(components[i]);
					}
					parentComponent += ".".concat(components[i]);
				}
			}
			jsonPathMapCache.putIfAbsent(jsonName, jsonPathMap);
			logger.debug("JsonPath map : %s", jsonPathMap);
			return jsonPathMap;
		}
	}

	/**
	 * Generates the tree which maps to json structure which will be used then to
	 * traverse and generate the records. Here, tree is n-ary tree which represenets
	 * json. Tree node can be of 3 types which can be Object, array or any primitive
	 * value.
	 * <p>
	 * If node is of type is Object, it represents JsonObject and it has
	 * corresponding JsonObject as value. If node is of type array, it as list of
	 * JsonObjects as the value of the node where each JsonObject represent each
	 * object in JsonArray. If node is of type primitive, it should be leaf node and
	 * represents record of the flattened json.
	 *
	 * @param jsonPathMap jsonPathMap generated using
	 *                    {@link JsonFlattener#toJsonPathMap()}
	 * @param rootNode    currently running node in tree.
	 * @return number of records under the node.
	 */
	private int generateTree(Map<String, Set<String>> jsonPathMap, TreeNode rootNode) {

		logger.debug("Generating tree for flattening json");
		Set<String> childNodeList = jsonPathMap.get(rootNode.getNodePath());

		int levelRecordCount = 1;
		if (childNodeList != null && !childNodeList.isEmpty()) {
			for (String childNodeName : childNodeList) {
				boolean isArray = childNodeName.contains("[*]");
				String jsonPath = "$.".concat(childNodeName);
				String childNodeJsonPath = rootNode.getNodePath().concat(".").concat(childNodeName);
				Object value = null;

				try {
					if (rootNode.getValue() != null) {
						value = JsonPath.read(rootNode.getValue(), jsonPath);
					}
				} catch (PathNotFoundException exp) {
					// catch the exception so that we can put null as the value
				}

				TreeNode childNode = new TreeNode(value, childNodeName, TreeNode.NodeType.OBJECT, childNodeJsonPath);
				rootNode.addChildNode(childNode);

				int records = 0;

				if (isArray) {
					// If array, visit each of the object in JsonArray and add it to the value of
					// the tree node.
					List<TreeNode> arrayNodes = new ArrayList<>();
					childNode.setValue(arrayNodes);
					childNode.setNodeType(TreeNode.NodeType.ARRAY);

					if (value != null) {
						JSONArray jsonArray = (JSONArray) value;
						for (Object eachJsonObject : jsonArray) {
							TreeNode arrayNode = new TreeNode(eachJsonObject, childNodeName, TreeNode.NodeType.OBJECT,
									childNode.getNodePath());
							arrayNodes.add(arrayNode);
							int nodeRecords = generateTree(jsonPathMap, arrayNode);
							arrayNode.setRecordCount(nodeRecords);
							records += nodeRecords;
						}
					} else {
						TreeNode arrayNode = new TreeNode(null, childNodeName, TreeNode.NodeType.OBJECT,
								childNode.getNodePath());
						arrayNodes.add(arrayNode);
						int nodeRecords = generateTree(jsonPathMap, arrayNode);
						arrayNode.setRecordCount(nodeRecords);
						records += nodeRecords;
					}

					childNode.setRecordCount(records);

				} else {
					boolean isObject = jsonPathMap.get(childNode.getNodePath()) != null;
					if (isObject) {
						int nodeRecords = generateTree(jsonPathMap, childNode);
						childNode.setRecordCount(nodeRecords);
						records = nodeRecords;
					} else {
						childNode.setNodeType(TreeNode.NodeType.PREMITIVE);
						childNode.setRecordCount(1);
						records = 1;
					}
				}

				levelRecordCount *= records;
			}
		}
		return levelRecordCount;
	}

	/**
	 * Create the records by traversing the whole tree in depth first manner and
	 * uses bottom up approach.
	 *
	 * @param rootNode root node of the tree generated using
	 *                 {@link JsonFlattener#generateTree(Map, TreeNode)}
	 * @return list of flattened records which is represented as
	 *         {@link JsonFlattener.JSONRecord}
	 */
	@SuppressWarnings("unchecked")
	private List<JSONRecord> createRecords(TreeNode rootNode) {

		logger.debug("Creating records for json");
		List<TreeNode> childNodes = rootNode.getChildNodes();

		for (TreeNode childNode : childNodes) {
			if (childNode.getNodeType() == TreeNode.NodeType.ARRAY) {
				// This is array of nodes, hence we need tovisit each of the array node and
				// create
				// records for each.
				List<TreeNode> arrayNodes = (List<TreeNode>) childNode.getValue();
				List<JSONRecord> recordsForArrayNodes = new ArrayList<>();
				for (TreeNode arrayNode : arrayNodes) {
					List<JSONRecord> records = createRecords(arrayNode);
					arrayNode.setRecords(records);
					// Records get added for each of the node in the array.
					recordsForArrayNodes.addAll(records);
				}
				childNode.setRecords(recordsForArrayNodes);
			} else {
				if (childNode.getChildNodes() == null) {
					// This is leaf node, hence required for our flattened record.
					if (childNode.getNodeType() != TreeNode.NodeType.OBJECT) {
						childNode.addRecord(new JSONRecord(childNode.getNodePath(), childNode.getValue()));
					}
				} else {
					// This is single object node. Create record by traversing childs under this
					// node.
					List<JSONRecord> records = createRecords(childNode);
					childNode.setRecords(records);
				}
			}
		}

		// Travers each element at the same level and do cross join of records of each
		// node.
		List<JSONRecord> recordList = new ArrayList<>();
		int prevRecordCount = 1;
		for (TreeNode childNode : childNodes) {
			List<JSONRecord> newRecordList = new ArrayList<>();
			for (int j = 0; j < prevRecordCount; j++) {
				for (int k = 0; k < childNode.getRecordCount(); k++) {
					// if it is first record, then we don't need to copy the record while merging as
					// it is already
					// there.
					JSONRecord record = recordList.isEmpty() ? new JSONRecord()
							: ((k == 0) ? recordList.get(j) : new JSONRecord().mergeRecord(recordList.get(j)));
					if (childNode.getRecords() != null) {
						record.mergeRecord(childNode.getRecords().get(k));
					}
					newRecordList.add(record);
				}
			}
			prevRecordCount = newRecordList.size();
			recordList = newRecordList;
		}
		return recordList;
	}

	/**
	 * Class to represent the node of the tree representing json.
	 */
	private static final class TreeNode {

		Object value;
		int recordCount;
		String nodeName;
		String nodePath;
		NodeType nodeType;
		List<TreeNode> childNodes;

		// list of records for the node
		List<JSONRecord> records;

		enum NodeType {
			ARRAY, OBJECT, PREMITIVE
		}

		TreeNode(Object value, String nodeName, NodeType nodeType) {

			this(nodeName, nodeType);
			this.value = value;
		}

		TreeNode(String nodeName, NodeType nodeType) {

			this.nodeName = nodeName;
			this.nodeType = nodeType;
		}

		TreeNode(Object value, String nodeName, NodeType nodeType, String nodePath) {

			this(value, nodeName, nodeType);
			this.nodePath = nodePath;
		}

		String getNodePath() {

			return nodePath;
		}

		@SuppressWarnings("unused")
		void setNodePath(String nodePath) {

			this.nodePath = nodePath;
		}

		Object getValue() {

			return value;
		}

		void setValue(Object value) {

			this.value = value;
		}

		@SuppressWarnings("unused")
		String getNodeName() {

			return nodeName;
		}

		@SuppressWarnings("unused")
		void setNodeName(String nodeName) {

			this.nodeName = nodeName;
		}

		NodeType getNodeType() {

			return nodeType;
		}

		void setNodeType(NodeType nodeType) {

			this.nodeType = nodeType;
		}

		List<TreeNode> getChildNodes() {

			return childNodes;
		}

		void addChildNode(TreeNode dataNode) {

			if (this.childNodes == null || this.childNodes.isEmpty()) {
				this.childNodes = new ArrayList<>();
			}
			this.childNodes.add(dataNode);
		}

		@SuppressWarnings("unused")
		void setChildNodes(List<TreeNode> nextNodes) {

			this.childNodes = nextNodes;
		}

		int getRecordCount() {

			return recordCount;
		}

		void setRecordCount(int recordCount) {

			this.recordCount = recordCount;
		}

		List<JSONRecord> getRecords() {

			return records;
		}

		void setRecords(List<JSONRecord> records) {

			this.records = records;
		}

		void addRecord(JSONRecord record) {

			if (this.records == null) {
				this.records = new ArrayList<>();
			}
			this.records.add(record);
		}
	}
}
