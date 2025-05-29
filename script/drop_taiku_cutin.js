load("script/utils.js");


function header() {
	return ["対空カットイン発動艦娘", "対空カットイン種別"];
}

function begin() { }

function getCutinName(kind) {
	switch(kind) {
		case 1:
			return "1:高角砲×2／電探";
		case 2:
			return "2:高角砲／電探";
		case 3:
			return "3:高角砲×2";
		case 4:
			return "4:大口径主砲／三式弾／対空電探／高射装置";
		case 5:
			return "5:特殊高角砲×2／対空電探";
		case 6:
			return "6:大口径主砲／三式弾／高射装置";
		case 7:
			return "7:高角砲／高射装置／対空電探";
		case 8:
			return "8:特殊高角砲／対空電探";
		case 9:
			return "9:高角砲／高射装置";
		case 10:
			return "10:高角砲／特殊機銃／対空電探";
		case 11:
			return "11:高角砲／特殊機銃 <摩耶改二>";
		case 12:
			return "12:特殊機銃／対空機銃(対空3以上)／対空電探";
		case 13:
			return "13:特殊高角砲／特殊機銃／対空電探";
		case 14:
			return "14:高角砲／対空機銃／対空電探";
		case 15:
			return "15:高角砲／対空機銃";
		case 16:
			return "16:高角砲／対空機銃／対空電探";
		case 17:
			return "17:高角砲／対空機銃";
		case 18:
			return "18:特殊機銃";
		case 19:
			return "19:高角砲(対空7以下)／特殊機銃";
		case 20:
			return "20:特殊機銃";
		case 21:
			return "21:高角砲／対空電探";
		case 22:
			return "22:特殊機銃";
		case 23:
			return "23:対空機銃(対空3～8)";
		case 24:
			return "24:高角砲／対空機銃(対空3～8)";
		case 25:
			return "25:噴進砲改二／対空電探／三式弾";
		case 26:
			return "26:高角砲改＋増設機銃／対空電探";
		case 27:
			return "27:高角砲改＋増設機銃／噴進砲改二／対空電探";
		case 28:
			return "28:噴進砲改二／対空電探";
		case 29:
			return "29:高角砲／対空電探";
		case 30:
			return "30:高角砲×3";
		case 31:
			return "31:高角砲×2";
		case 32:
			return "32:20連装ロケラン×2 or 20連装ロケラン／ポンポン砲 or FCRtype284／ポンポン砲";
		case 33:
			return "33:高角砲／対空機銃(対空4以上)";
		case 34:
			return "34:Fletcher砲改+GFCS×2";
		case 35:
			return "35:Fletcher砲改+GFCS／Fletcher砲(改)";
		case 36:
			return "36:Fletcher砲(改)×2／GFCS電探";
		case 37:
			return "37:Fletcher砲(改)×2";
		case 38:
			return "38:GFCS+Atlanta砲×2";
		case 39:
			return "39:GFCS+Atlanta砲／Atlanta砲";
		case 40:
			return "40:(GFCS+)Atlanta砲×2／GFCS電探";
		case 41:
			return "41;(GFCS+)Atlanta砲×2";
		case 42:
			return "42:連装高角砲群 集中配備×2／測距儀電探／対空機銃(対空6以上)";
		case 43:
			return "43:連装高角砲群 集中配備×2／測距儀電探";
		case 44:
			return "44:連装高角砲群 集中配備／測距儀電探／対空機銃(対空6以上)";
		case 45:
			return "45:連装高角砲群 集中配備／測距儀電探";
		case 46:
			return "46:35.6cm連装砲改四 or 35.6cm連装砲改三(ダズル迷彩仕様)／特殊機銃／対空電探";
		case 47:
			return "47:12.7cm連装砲C型改三H／12.7cm連装砲C型改三H or 25mm対空機銃増備 or 対空電探(対空4以上)";
		case 48:
			return "48:10cm連装高角砲改＋高射装置改×2／対空電探(対空4以上)";
		case 49:
			return "49:特殊高角砲×2／対空電探(対空4以上)";
		case 50:
			return "50:10cm連装高角砲改(＋高射装置改)×2／対空電探(対空4以上)／94式高射装置";
		case 51:
			return "51:10cm連装高角砲改(＋高射装置改)／対空電探(対空4以上)／対空機銃(対空5以上)";
		case 52:
			return "52:10cm連装高角砲改×2／94式高射装置";

		default:
			return kind + ":新しいカットインです";
	}
}

function body(battle) {
	var shipName = null;
	var kindName = null;
	var p1json = battle.getPhase1().getJson();
	if( p1json != null &&
	    p1json.api_kouku != null &&
		p1json.api_kouku.api_stage2 != null &&
		p1json.api_kouku.api_stage2.api_air_fire != null)
	{
		var air_fire = p1json.api_kouku.api_stage2.api_air_fire;
		var idx = air_fire.api_idx.intValue();
		shipName = (battle.isCombined() && idx >= 6)
			? battle.getDockCombined().getShips().get(idx - 6).getFriendlyName()
			: battle.getDock().getShips().get(idx).getFriendlyName();
		kindName = getCutinName(air_fire.api_kind.intValue());
	}
	return toComparable([shipName, kindName]);
}

function end() { }
