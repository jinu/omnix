var StringUtils = Java.type('org.apache.commons.lang3.StringUtils');
var ParseUtils = Java.type('com.omnix.util.ParseUtils');
var HashMap = Java.type('java.util.HashMap');
var ArrayList = Java.type('java.util.ArrayList');
var CSVUtils = Java.type('com.omnix.util.CSVUtils');

var CSVFormat = Java.type('org.apache.commons.csv.CSVFormat');
var CSVParser = Java.type('org.apache.commons.csv.CSVParser');

var MappingManager = Java.type('com.omnix.manager.parser.MappingInfoManager');

var CSVFormatDefault = CSVFormat.DEFAULT.withTrim().withIgnoreHeaderCase();


function parseSimpleCsv(text) {
	return CSVUtils.parseLine(text);
}

function parseSimpleCsvSpace(text) {
	return CSVUtils.parseLineSpace(text);
}

function parseSplitComma(text) {
	return ParseUtils.splitComma(text);
}

function parseSplitSpace(text) {
	return ParseUtils.splitSpace(text);
}

function parseCsv(text) {
	return CSVParser.parse(text, CSVFormatDefault).getRecords().get(0);
}


function parseSimpleMap(text, delim, seperator) {
	return ParseUtils.splitToMap(text, delim, seperator);
}

function parseMap(text, delim, seperator, escape) {
	return ParseUtils.splitToMap(text, delim, seperator, escape);
}

function parseDate(text, pattern) {
	return ParseUtils.dateToLong(text, pattern);
}

function mapping(key, name, defaultValue) {
	return MappingManager.mapping(key, name, defaultValue);
}

function getCountryCode(ip) {
	return ParseUtils.getCountryCode(ip);
}