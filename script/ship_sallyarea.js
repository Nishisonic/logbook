load("script/utils.js");

function header() {
	return [ "出撃海域" ];
}

function begin(specdiff) { }

function sallyArea(area) {
	if (area > 0) {
		return new java.lang.String("札" + String.fromCharCode("A".charCodeAt() + area - 1));
	}
	return null;
}

function body(ship) {
	if(ship.json == null || isJsonNull(ship.json.api_sally_area))
		return null;
	return toComparable([ sallyArea(ship.json.api_sally_area.intValue()) ]);
}

function end() { }
