package com.ninja.BankStAnalysis.core.ananomyzer.core;

import com.google.common.base.Joiner;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class to represent the raw of the flattened json.
 */
public class JSONRecord {
	Map<String, Object> cells;

	public JSONRecord() {

		cells = new LinkedHashMap<>();
	}

	public JSONRecord(String columName, Object columnValue) {

		this.cells = new LinkedHashMap<>();
		if (columnValue == null) {
			columnValue = "null";
		}
		this.cells.put(columName, columnValue);
	}

	public Map<String, Object> getCells() {

		return cells;
	}

	public JSONRecord mergeRecord(JSONRecord record) {

		this.cells.putAll(record.getCells());
		return this;
	}

	@SuppressWarnings("unused")
	public void addCell(String columName, Object columnValue) {

		if (this.cells == null) {
			this.cells = new LinkedHashMap<>();
		}
		this.cells.put(columName, columnValue);
	}

	public String toString(char separator) {

		if (cells == null || cells.values() == null) {
			return null;
		}
		return Joiner.on(separator).join(cells.values());
	}

	@Override
	public String toString() {

		return toString(',');
	}
}
