/**
 *
 */
package logbook.dto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import com.dyuproject.protostuff.Tag;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import logbook.config.AppConfig;
import logbook.data.DataType;
import logbook.data.context.GlobalContext;
import logbook.gui.logic.DateTimeString;
import logbook.internal.EnemyData;
import logbook.internal.Item;
import logbook.internal.LoggerHolder;
import logbook.internal.UseItem;
import logbook.scripting.BuiltinScriptFilter;
import logbook.util.GsonUtil;
import logbook.util.JsonUtils;

/**
 * １回の会敵情報
 * @author Nekopanda
 */
public class BattleExDto extends AbstractDto {
    private static LoggerHolder LOG = new LoggerHolder("builtinScript");

    /** 日付 */
    @Tag(1)
    private final Date battleDate;

    /** 味方艦隊 */
    @Tag(2)
    private final List<DockDto> friends = new ArrayList<>();

    /** 敵艦隊 */
    @Tag(3)
    private final List<EnemyShipDto> enemy = new ArrayList<>();

    /** 敵随伴艦隊 */
    @Tag(31)
    private List<EnemyShipDto> enemyCombined = new ArrayList<>();

    /** 味方MaxHP */
    @Tag(6)
    private int[] maxFriendHp;

    @Tag(7)
    private int[] maxFriendHpCombined;

    /** 敵MaxHP */
    @Tag(8)
    private int[] maxEnemyHp;

    @Tag(32)
    private int[] maxEnemyHpCombined;

    /** 味方戦闘開始時HP */
    @Tag(9)
    private int[] startFriendHp;

    @Tag(10)
    private int[] startFriendHpCombined;

    /** 敵戦闘開始時HP */
    @Tag(11)
    private int[] startEnemyHp;

    @Tag(33)
    private int[] startEnemyHpCombined;

    /** 戦闘前の味方総HP */
    @Tag(12)
    private int friendGaugeMax = 0;

    /** 戦闘前の敵総HP */
    @Tag(13)
    private int enemyGaugeMax = 0;

    /** 陣形（味方・敵） */
    @Tag(14)
    private final String[] formation = new String[] { "陣形不明", "陣形不明" };

    /** 同航戦とか　*/
    @Tag(15)
    private String formationMatch = "不明";

    /** 索敵状態（味方・敵） */
    @Tag(16)
    private String sakuteki[];

    /** 攻撃フェーズ */
    @Tag(17)
    private final List<Phase> phaseList = new ArrayList<Phase>();

    /** 海域名 */
    @Tag(18)
    private String questName;

    /** ランク */
    @Tag(19)
    private ResultRank rank;

    /** マス */
    @Tag(20)
    private MapCellDto mapCellDto;

    /** 敵艦隊名 */
    @Tag(21)
    private String enemyName;

    /** ドロップフラグ */
    @Tag(22)
    private boolean dropShip;

    /** ドロップフラグ */
    @Tag(39)
    private boolean dropItem;

    /** 艦種 */
    @Tag(23)
    private String dropType;

    /** 艦名 */
    @Tag(24)
    private String dropName;

    /** ドロップ艦ID */
    @Tag(46)
    private int dropShipId;

    /** アイテム名 */
    @Tag(47)
    private String dropItemName;

    /** MVP艦（ゼロ始まりのインデックス） */
    @Tag(25)
    private int mvp;

    @Tag(26)
    private int mvpCombined;

    /** 提督Lv */
    @Tag(27)
    private int hqLv;

    /**
     * BattleExDtoのバージョン
     * exVersion == 0 : Tag 34以降がない
     * exVersion == 1 : Tag 36まである
     * exVersion == 2 : Jsonがある
     *  */
    @Tag(34)
    private int exVersion = 2;

    /** 母港空き（ドロップ分を含まない） */
    @Tag(35)
    private int shipSpace;

    /** 装備空き（ドロップ分を含まない） */
    @Tag(36)
    private int itemSpace;

    /** 連合艦隊における退避意見 [退避する艦(0-11), 護衛艦(0-11)] */
    @Tag(37)
    private int[] escapeInfo;

    @Tag(38)
    private boolean[] escaped;

    /** 轟沈フラグ */
    @Tag(44)
    private boolean[] lostflag;

    /** 次の連合艦隊ではこうなりそう(ならなかったら轟沈艦を取り除くことができないので処理を再考する必要あり) */
    //@Tag(45)
    //private boolean[] lostflagCombined;

    @Tag(51)
    private String resultJson;

    /** 連合艦隊の種類 */
    @Tag(52)
    private int combinedKind = 0;

    /////////////////////////////////////////////////

    /**
     * 戦闘1フェーズの情報
     * @author Nekopanda
     */
    public static class Phase {

        @Tag(1)
        private final BattlePhaseKind kind;
        /** 味方HP */
        @Tag(2)
        private final int[] nowFriendHp;

        @Tag(3)
        private final int[] nowFriendHpCombined;

        /** 敵HP */
        @Tag(4)
        private final int[] nowEnemyHp;

        @Tag(21)
        private final int[] nowEnemyHpCombined;

        /** ランク */
        @Tag(5)
        private ResultRank estimatedRank;

        /** 夜戦 */
        @Tag(6)
        private final boolean isNight;

        /** 敵は連合艦隊第二艦隊か？ */
        @Tag(22)
        private final boolean isEnemySecond;

        /** 自分は連合艦隊編成か？（第二艦隊が存在するか？） */
        @Tag(100)
        private final boolean isFriendSecond;

        /** 支援攻撃のタイプ */
        @Tag(7)
        private String supportType;

        /** 触接機（味方・敵） -1の場合は「触接なし」 */
        @Tag(8)
        private int[] touchPlane;

        /** 照明弾発射艦 */
        @Tag(32)
        private int[] flarePos;

        @Tag(9)
        private String seiku;

        /** 損害率（味方・敵） */
        @Tag(10)
        private double[] damageRate;

        /** 攻撃シーケンス */
        @Tag(36)
        private AirBattleDto airBaseInjection = null;
        @Tag(37)
        private AirBattleDto airInjection = null;
        @Tag(35)
        private List<AirBattleDto> airBase = null;
        @Tag(11)
        private AirBattleDto air = null;
        @Tag(12)
        private AirBattleDto air2 = null;
        @Tag(13)
        private List<BattleAtackDto> support = null;
        @Tag(50)
        private List<BattleAtackDto> openingTaisen = null;
        @Tag(14)
        private List<BattleAtackDto> opening = null;
        @Tag(15)
        private List<BattleAtackDto> raigeki = null;
        @Tag(16)
        private List<BattleAtackDto> hougeki = null;
        @Tag(17)
        private List<BattleAtackDto> hougeki1 = null;
        @Tag(18)
        private List<BattleAtackDto> hougeki2 = null;
        @Tag(19)
        private List<BattleAtackDto> hougeki3 = null;

        @Tag(30)
        private final String json;

        public Phase(BattleExDto battle, JsonObject object, BattlePhaseKind kind,
                int[] beforeFriendHp, int[] beforeFriendHpCombined, int[] beforeEnemyHp, int[] beforeEnemyHpCombined) {
            boolean isFriendCombined = (beforeFriendHpCombined != null);
            boolean isEnemyCombined = (beforeEnemyHpCombined != null);

            // 敵は連合艦隊の第二艦隊か？（敵連合艦隊夜戦で第二艦隊が相手の場合のみ）
            this.isEnemySecond = (object.containsKey("api_active_deck")
                    ? (object.getJsonArray("api_active_deck").getInt(1) == 2) : false);

            // 自分は連合艦隊の第二艦隊か？（自連合艦隊 vs 敵連合艦隊の夜戦で
            // 自分が第二艦隊であることの確認）
            this.isFriendSecond = (object.containsKey("api_active_deck")
                    ? (object.getJsonArray("api_active_deck").getInt(0) == 2) : false);

            this.kind = kind;
            this.isNight = kind.isNight();

            this.nowFriendHp = beforeFriendHp.clone();
            this.nowEnemyHp = beforeEnemyHp.clone();
            this.nowFriendHpCombined = isFriendCombined ? beforeFriendHpCombined.clone() : null;
            this.nowEnemyHpCombined = isEnemyCombined ? beforeEnemyHpCombined.clone() : null;

            // 夜間触接
            JsonArray jsonTouchPlane = object.getJsonArray("api_touch_plane");
            if (jsonTouchPlane != null) {
                this.touchPlane = new int[] {
                        Integer.parseInt(jsonTouchPlane.get(0).toString()),
                        Integer.parseInt(jsonTouchPlane.get(1).toString()),
                };
            }

            // 照明弾発射艦
            JsonValue jsonFlarePos = object.get("api_flare_pos");
            if ((jsonFlarePos != null) && (jsonFlarePos != JsonValue.NULL)) {
                this.flarePos = JsonUtils.getIntArray(object, "api_flare_pos");
            }

            // 攻撃シーケンスを読み取る //
            // 基地航空隊（墳式強襲）
            JsonObject air_base_injection = object.getJsonObject("api_air_base_injection");
            if (air_base_injection != null) {
                this.airBaseInjection = new AirBattleDto(air_base_injection, isFriendCombined || isEnemyCombined, true);
            }

            // 航空戦（墳式強襲）
            JsonObject injection_kouku = object.getJsonObject("api_injection_kouku");
            if (injection_kouku != null) {
                this.airInjection = new AirBattleDto(injection_kouku, isFriendCombined || isEnemyCombined, false);
            }

            // 基地航空隊
            JsonValue air_base_attack = object.get("api_air_base_attack");
            if (air_base_attack instanceof JsonArray) {
                this.airBase = new ArrayList<>();
                for (JsonValue elem : (JsonArray) air_base_attack) {
                    JsonObject obj = (JsonObject) elem;
                    this.airBase.add(new AirBattleDto(obj, isFriendCombined || isEnemyCombined, true));
                }
            }

            // 航空戦（通常）
            JsonObject kouku = object.getJsonObject("api_kouku");
            if (kouku != null) {
                this.air = new AirBattleDto(kouku, isFriendCombined || isEnemyCombined, false);
                // 昼戦の触接はここ
                this.touchPlane = this.air.touchPlane;
                // 制空はここから取る
                this.seiku = this.air.seiku;
            }

            // 支援艦隊
            JsonNumber support_flag = object.getJsonNumber("api_support_flag");
            if ((support_flag != null) && (support_flag.intValue() != 0)) {
                JsonObject support = object.getJsonObject("api_support_info");
                JsonValue support_hourai = support.get("api_support_hourai");
                JsonValue support_air = support.get("api_support_airatack");
                if ((support_hourai != null) && (support_hourai != JsonValue.NULL)) {
                    JsonArray edam = ((JsonObject) support_hourai).getJsonArray("api_damage");
                    if (edam != null) {
                        this.support = BattleAtackDto.makeSupport(edam);
                    }
                }
                else if ((support_air != null) && (support_air != JsonValue.NULL)) {
                    JsonValue stage3 = ((JsonObject) support_air).get("api_stage3");
                    if ((stage3 != null) && (stage3 != JsonValue.NULL)) {
                        this.support = BattleAtackDto.makeSupport(((JsonObject) stage3).getJsonArray("api_edam"));
                    }
                }
                this.supportType = toSupport(support_flag.intValue());
            }
            else {
                this.supportType = "";
            }

            // 航空戦（連合艦隊のみ？）
            JsonObject kouku2 = object.getJsonObject("api_kouku2");
            if (kouku2 != null)
                this.air2 = new AirBattleDto(kouku2, isFriendCombined || isEnemyCombined, false);

            // 開幕対潜
            this.openingTaisen = BattleAtackDto.makeHougeki(object.get("api_opening_taisen"), kind.isOpeningSecond(),
                    this.isEnemySecond);

            // 開幕
            this.opening = BattleAtackDto.makeRaigeki(object.get("api_opening_atack"), kind.isOpeningSecond());

            // 砲撃
            if (this.isFriendSecond) {
                this.hougeki = BattleAtackDto.makeHougeki(object.get("api_hougeki"),
                        true, this.isEnemySecond); // 自分が連合艦隊の場合の夜戦
            }
            else {
                this.hougeki = BattleAtackDto.makeHougeki(object.get("api_hougeki"),
                        kind.isHougekiSecond(), this.isEnemySecond); // 夜戦
            }
            this.hougeki1 = BattleAtackDto.makeHougeki(object.get("api_hougeki1"),
                    kind.isHougeki1Second(), this.isEnemySecond);
            this.hougeki2 = BattleAtackDto.makeHougeki(object.get("api_hougeki2"),
                    kind.isHougeki2Second(), this.isEnemySecond);
            this.hougeki3 = BattleAtackDto.makeHougeki(object.get("api_hougeki3"),
                    kind.isHougeki3Second(), this.isEnemySecond);

            // 雷撃
            this.raigeki = BattleAtackDto.makeRaigeki(object.get("api_raigeki"), kind.isRaigekiSecond());

            // ダメージを反映 //
            if (this.airBaseInjection != null)
                this.doAtack(this.airBaseInjection.atacks);
            if (this.airInjection != null)
                this.doAtack(this.airInjection.atacks);
            if (this.airBase != null)
                for (AirBattleDto attack : this.airBase)
                    this.doAtack(attack.atacks);

            if (this.air != null)
                this.doAtack(this.air.atacks);
            this.doAtack(this.support);
            if (this.air2 != null)
                this.doAtack(this.air2.atacks);
            this.doAtack(this.openingTaisen);
            this.doAtack(this.opening);
            this.doAtack(this.hougeki);
            this.doAtack(this.hougeki1);
            this.doAtack(this.raigeki);
            this.doAtack(this.hougeki2);
            this.doAtack(this.hougeki3);

            this.json = object.toString();
        }

        public Phase(BattleExDto battle, LinkedTreeMap tree,String json, BattlePhaseKind kind,
                int[] beforeFriendHp, int[] beforeFriendHpCombined, int[] beforeEnemyHp, int[] beforeEnemyHpCombined)
        {
            boolean isFriendCombined = (beforeFriendHpCombined != null);
            boolean isEnemyCombined = (beforeEnemyHpCombined != null);

            // 敵は連合艦隊の第二艦隊か？（敵連合艦隊夜戦で第二艦隊が相手の場合のみ）
            this.isEnemySecond = (tree.containsKey("api_active_deck")) ?(GsonUtil.toIntArray(tree.get("api_active_deck"))[1] == 2) : false;
            this.isFriendSecond = (tree.containsKey("api_active_deck")
                    ? (GsonUtil.toIntArray(tree.get("api_active_deck"))[0] == 2) : false);

            this.kind = kind;
            this.isNight = kind.isNight();

            this.nowFriendHp = beforeFriendHp.clone();
            this.nowEnemyHp = beforeEnemyHp.clone();
            this.nowFriendHpCombined = isFriendCombined ? beforeFriendHpCombined.clone() : null;
            this.nowEnemyHpCombined = isEnemyCombined ? beforeEnemyHpCombined.clone() : null;

            // 夜間触接
            int[] jsonTouchPlane = GsonUtil.toIntArray(tree.get("api_touch_plane"));
            this.touchPlane = jsonTouchPlane;

            // 照明弾発射艦
            int[] jsonFlarePos = GsonUtil.toIntArray(tree.get("api_flare_pos"));
            this.flarePos = jsonFlarePos;

            // 攻撃シーケンスを読み取る //
            LinkedTreeMap air_base_injection = (LinkedTreeMap)tree.get("api_air_base_injection");
            if(air_base_injection != null){
                this.airBaseInjection = new AirBattleDto(air_base_injection, isFriendCombined || isEnemyCombined, true);
            }
            LinkedTreeMap air_injection_kouku = (LinkedTreeMap)tree.get("api_injection_kouku");
            if(air_injection_kouku != null){
                this.airInjection = new AirBattleDto(air_injection_kouku, isFriendCombined || isEnemyCombined, false);
            }
            // 基地航空隊
            Object air_base_attack = tree.get("api_air_base_attack");
            if (air_base_attack instanceof List) {
                this.airBase = new ArrayList<>();
                for (Object item : (List)air_base_attack) {
                    this.airBase.add(new AirBattleDto((LinkedTreeMap)item, isFriendCombined || isEnemyCombined, true));
                }
            }

            // 航空戦（通常）
            LinkedTreeMap kouku = (LinkedTreeMap)tree.get("api_kouku");
            if (kouku != null) {
                this.air = new AirBattleDto(kouku, isFriendCombined || isEnemyCombined, false);
                // 昼戦の触接はここ
                this.touchPlane = this.air.touchPlane;
                // 制空はここから取る
                this.seiku = this.air.seiku;
            }

            // 支援艦隊
            int support_flag = GsonUtil.toInt(tree.get("api_support_flag"));
            if (support_flag > 0) {
                LinkedTreeMap support = (LinkedTreeMap)tree.get("api_support_info");
                LinkedTreeMap support_hourai = (LinkedTreeMap)support.get("api_support_hourai");
                LinkedTreeMap support_air = (LinkedTreeMap)support.get("api_support_airatack");
                if (support_hourai != null) {
                    int[] edam = GsonUtil.toIntArray(support_hourai.get("api_damage"));
                    int[] ecl = GsonUtil.toIntArray(support_hourai.get("api_cl_list"));
                    if (edam != null) {
                        this.support = BattleAtackDto.makeSupport(edam,ecl);
                    }
                }
                else if (support_air != null) {
                    LinkedTreeMap stage3 = (LinkedTreeMap)support_air.get("api_stage3");
                    if (stage3 != null) {
                        this.support = BattleAtackDto.makeSupportAir(GsonUtil.toIntArray(stage3.get("api_edam")),GsonUtil.toIntArray(stage3.get("api_ecl_flag")));
                    }
                }
                this.supportType = toSupport(support_flag);
            }
            else {
                this.supportType = "";
            }

            //航空戦
            LinkedTreeMap kouku2 = (LinkedTreeMap)tree.get("api_kouku2");
            if (kouku2 != null){
                this.air2 = new AirBattleDto(kouku2, isFriendCombined || isEnemyCombined, false);
            }

            // 開幕対潜
            this.openingTaisen = BattleAtackDto.makeHougeki((LinkedTreeMap)tree.get("api_opening_taisen"), kind.isOpeningSecond(),
                    this.isEnemySecond);

            // 開幕
            this.opening = BattleAtackDto.makeRaigeki((LinkedTreeMap)tree.get("api_opening_atack"), kind.isOpeningSecond());

            // 砲撃
            this.hougeki = BattleAtackDto.makeHougeki((LinkedTreeMap)tree.get("api_hougeki"), isCombined(),
                    this.isEnemySecond); // 夜戦
            this.hougeki1 = BattleAtackDto.makeHougeki((LinkedTreeMap)tree.get("api_hougeki1"), kind.isHougeki1Second(),
                    this.isEnemySecond);
            this.hougeki2 = BattleAtackDto.makeHougeki((LinkedTreeMap)tree.get("api_hougeki2"), kind.isHougeki2Second(),
                    this.isEnemySecond);
            this.hougeki3 = BattleAtackDto.makeHougeki((LinkedTreeMap)tree.get("api_hougeki3"), kind.isHougeki3Second(),
                    this.isEnemySecond);

            // 雷撃
            this.raigeki = BattleAtackDto.makeRaigeki((LinkedTreeMap)tree.get("api_raigeki"), kind.isRaigekiSecond());

            // ダメージを反映 //
            if (this.airBaseInjection != null){
                this.doAtack(this.airBaseInjection.atacks);
            }
            if (this.airInjection != null){
                this.doAtack(this.airInjection.atacks);
            }
            if (this.airBase != null)
                for (AirBattleDto attack : this.airBase)
                    this.doAtack(attack.atacks);
            if (this.air != null)
                this.doAtack(this.air.atacks);
            this.doAtack(this.support);
            if (this.air2 != null)
                this.doAtack(this.air2.atacks);
            this.doAtack(this.openingTaisen);
            this.doAtack(this.opening);
            this.doAtack(this.hougeki);
            this.doAtack(this.hougeki1);
            this.doAtack(this.raigeki);
            this.doAtack(this.hougeki2);
            this.doAtack(this.hougeki3);

            this.json = json;
        }


        public void battleDamage(BattleExDto battle) {
            int numFships = this.nowFriendHp.length;
            int numEships = this.nowEnemyHp.length;
            DockDto dock = battle.getDock();
            DockDto dockCombined = battle.getDockCombined();

            // HP0以下を0にする
            for (int i = 0; i < numFships; i++) {
                this.nextHp(i, this.nowFriendHp, (dock != null) ? dock.getShips() : null);
            }
            for (int i = 0; i < numEships; i++) {
                if (this.nowEnemyHp[i] <= 0)
                    this.nowEnemyHp[i] = 0;
            }
            if (this.nowFriendHpCombined != null) {
                for (int i = 0; i < this.nowFriendHpCombined.length; i++) {
                    this.nextHp(i, this.nowFriendHpCombined, (dockCombined != null) ? dockCombined.getShips() : null);
                }
            }
            if (this.nowEnemyHpCombined != null) {
                for (int i = 0; i < this.nowEnemyHpCombined.length; i++) {
                    if (this.nowEnemyHpCombined[i] <= 0)
                        this.nowEnemyHpCombined[i] = 0;
                }
            }

            // 判定を計算
            this.estimatedRank = this.calcResultRank(battle);
        }

        public void practiceDamage(BattleExDto battle) {
            int numFships = this.nowFriendHp.length;
            int numEships = this.nowEnemyHp.length;
            boolean isCombined = (this.nowFriendHpCombined != null);
            int numFshipsCombined = isCombined ? this.nowFriendHpCombined.length : 0;

            // HP0以下を0にする
            for (int i = 0; i < numFships; i++) {
                if (this.nowFriendHp[i] <= 0)
                    this.nowFriendHp[i] = 0;
            }
            for (int i = 0; i < numEships; i++) {
                if (this.nowEnemyHp[i] <= 0)
                    this.nowEnemyHp[i] = 0;
            }
            if (isCombined) {
                for (int i = 0; i < numFshipsCombined; i++) {
                    if (this.nowFriendHpCombined[i] <= 0)
                        this.nowFriendHpCombined[i] = 0;
                }
            }

            // 判定を計算
            this.estimatedRank = this.calcResultRank(battle);
        }

        private void nextHp(int index, int[] hps, List<ShipDto> ships) {
            int hp = hps[index];
            if (ships == null) {
                if (hp <= 0) {
                    hps[index] = 0;
                }
                return;
            }
            ShipDto ship = ships.get(index);
            if (hp <= 0) {
                List<ItemDto> items = new ArrayList<>(ship.getItem2());
                items.add(ship.getSlotExItem());
                for (ItemDto item : items) {
                    if (item == null)
                        continue;
                    if (item.getSlotitemId() == 42) { //応急修理要員
                        hps[index] = (int) (ship.getMaxhp() * 0.2);
                        return;
                    }
                    else if (item.getSlotitemId() == 43) { //応急修理女神
                        hps[index] = ship.getMaxhp();
                        return;
                    }
                }
                hps[index] = 0;
                return;
            }
            return;
        }

        // 勝利判定 //
        private ResultRank calcResultRank(BattleExDto battle) {
            boolean isFriendCombined = (this.nowFriendHpCombined != null);
            boolean isEnemyCombined = (this.nowEnemyHpCombined != null);
            int numFships = this.nowFriendHp.length;
            int numFshipsCombined = isFriendCombined ? this.nowFriendHpCombined.length : 0;
            int numEships = this.nowEnemyHp.length;
            int numEshipsCombined = isEnemyCombined ? this.nowEnemyHpCombined.length : 0;
            int[] nowFriendHp = this.nowFriendHp;
            int[] nowFriendHpCombined = this.nowFriendHpCombined;
            int[] nowEnemyHp = this.nowEnemyHp;
            int[] nowEnemyHpCombined = this.nowEnemyHpCombined;
            int friendEscaped = 0;

            // 自艦隊の戦闘終了時のHPの合計(A)
            int friendGauge = IntStream.range(0, numFships)
                    .filter(i -> !((battle.escaped != null) && battle.escaped[i]))
                    .map(i -> nowFriendHp[i]).sum();

            // 自艦隊の戦闘開始時のHPの合計(B)
            int friendGaugeMax = battle.friendGaugeMax;

            // 自艦隊の戦闘終了後の生存数
            int friendNowShips = (int) Arrays.stream(nowFriendHp).filter(hp -> hp > 0).count();

            // 連合艦隊(自艦隊)
            if (isFriendCombined) {
                friendGauge += IntStream.range(0, numFshipsCombined)
                        .filter(i -> !((battle.escaped != null) && battle.escaped[i + 6]))
                        .map(i -> nowFriendHpCombined[i]).sum();
                friendNowShips += (int) Arrays.stream(nowFriendHpCombined).filter(hp -> hp > 0).count();
                friendEscaped = (int) (battle.escaped != null ? IntStream.range(0, battle.escaped.length)
                        .mapToObj(i -> battle.escaped[i]).filter(escaped -> escaped).count() : 0);
            }

            // 自艦隊の轟沈数(C) (生存艦には退避した艦も含まれていることに注意)
            int friendSunk = (numFships + numFshipsCombined) - friendNowShips;

            // 敵艦隊の戦闘終了時のHPの合計(D)
            int enemyGauge = Arrays.stream(nowEnemyHp).sum();

            // 敵艦隊の戦闘開始時のHPの合計(E)
            int enamyGaugeMax = battle.enemyGaugeMax;

            // 敵艦隊の戦闘終了後の生存数
            int enemyNowShips = (int) Arrays.stream(nowEnemyHp).filter(hp -> hp > 0).count();

            // 連合艦隊(敵艦隊)
            if (isEnemyCombined) {
                enemyGauge += Arrays.stream(nowEnemyHpCombined).sum();
                enemyNowShips += (int) Arrays.stream(nowEnemyHpCombined).filter(hp -> hp > 0).count();
            }

            // 敵艦隊の轟沈数(F)
            int enemySunk = (numEships + numEshipsCombined) - enemyNowShips;

            // 自艦隊の戦闘開始時の艦船数(G)
            int numStartFships = (numFships + numFshipsCombined) - friendEscaped;

            // 敵艦隊の戦闘開始時の艦船数(H)
            int numStartEships = numEships + numEshipsCombined;

            this.damageRate = new double[] {
                    (double) (friendGaugeMax - friendGauge) / friendGaugeMax,
                    (double) (enamyGaugeMax - enemyGauge) / enamyGaugeMax,
            };

            double friendGaugeRate = Math.floor(this.damageRate[0] * 100);
            double enemyGaugeRate = Math.floor(this.damageRate[1] * 100);

            if ((this.kind == BattlePhaseKind.LD_AIRBATTLE) ||
                    (this.kind == BattlePhaseKind.COMBINED_LD_AIR)) {
                // 空襲戦
                // S勝利は発生しないと思われる(完全勝利Sのみ)
                if (friendGaugeMax <= friendGauge) {
                    return ResultRank.PERFECT;
                }
                if (friendGaugeRate < 10) {
                    return ResultRank.A;
                }
                if (friendGaugeRate < 20) {
                    return ResultRank.B;
                }
                if (friendGaugeRate < 50) {
                    return ResultRank.C;
                }
                if (friendGaugeRate < 80) {
                    return ResultRank.D;
                }
                return ResultRank.E;
            }
            else {
                // PHASE1:轟沈艦なし かつ 敵艦全滅
                if ((friendSunk == 0) && (enemySunk == numStartEships)) {
                    // 戦闘終了時のHPが戦闘開始時のHP以上の場合、完全勝利S判定にする
                    // ソースはscenes/BattleResultMain.swfのgetTweenShowRank()参照
                    if (friendGaugeMax <= friendGauge) {
                        return ResultRank.PERFECT;
                    }
                    else {
                        return ResultRank.S;
                    }
                }
                // PHASE2:轟沈艦なし かつ 敵艦隊の戦闘開始時の数が1隻より上 かつ 敵艦の撃沈数が7割以上
                else if ((friendSunk == 0) && (numStartEships > 1)
                        && (enemySunk >= Math.floor(0.7 * numStartEships))) {
                    return ResultRank.A;
                }
                // PHASE3:自艦隊の轟沈数より敵艦隊の撃沈数の方が多い かつ 敵旗艦撃沈
                else if ((friendSunk < enemySunk) && (nowEnemyHp[0] == 0)) {
                    return ResultRank.B;
                }
                // PHASE4:自艦隊が1隻のみ かつ 自旗艦大破
                else if ((numStartFships == 1) && (((double) nowFriendHp[0] / battle.getMaxFriendHp()[0]) <= 0.25)) {
                    return ResultRank.D;
                }
                // PHASE5:敵艦隊の損害率が自艦隊の損害率を2.5倍しても上なら
                else if (enemyGaugeRate > (2.5 * friendGaugeRate)) {
                    return ResultRank.B;
                }
                // PHASE6:敵艦隊の損害率が自艦隊の損害率を0.9倍しても上なら
                else if (enemyGaugeRate > (0.9 * friendGaugeRate)) {
                    return ResultRank.C;
                }
                // PHASE7:開始時2隻以上 かつ 旗艦以外全滅
                else if ((numStartFships > 1) && ((numStartFships - 1) == friendSunk)) {
                    return ResultRank.E;
                }
                // 残りはD
                else {
                    return ResultRank.D;
                }
            }
        }

        // ダメージを反映
        private void doAtack(List<BattleAtackDto> seq) {
            if (seq == null)
                return;

            for (BattleAtackDto dto : seq) {
                for (int i = 0; i < dto.target.length; ++i) {
                    int target = dto.target[i];
                    int damage = dto.damage[i];
                    if (dto.friendAtack) {
                        if (target < 6) {
                            this.nowEnemyHp[target] -= damage;
                        }
                        else {
                            this.nowEnemyHpCombined[target - 6] -= damage;
                        }
                    }
                    else {
                        if (target < 6) {
                            this.nowFriendHp[target] -= damage;
                        }
                        else {
                            this.nowFriendHpCombined[target - 6] -= damage;
                        }
                    }
                }
            }
        }

        /**
         * 連合艦隊か？
         * @return
         */
        public boolean isCombined() {
            return (this.nowFriendHpCombined != null);
        }

        /**
         * 航空戦情報 [１回目, 2回目]
         * 2回目は連合艦隊航空戦マスでの戦闘のみ
         * @return
         */
        public AirBattleDto[] getAirBattleDto() {
            return new AirBattleDto[] {
                    this.air, this.air2
            };
        }

        private BattleAtackDto[] toArray(List<BattleAtackDto> list) {
            return list.toArray(new BattleAtackDto[list.size()]);
        }

        private BattleAtackDto[] getAirBaseBattlesArray() {
            if (this.airBase == null) {
                return null;
            }
            List<BattleAtackDto> arr = new ArrayList<>();
            for (AirBattleDto dto : this.airBase) {
                if (dto.atacks != null) {
                    arr.addAll(dto.atacks);
                }
            }
            return arr.toArray(new BattleAtackDto[0]);
        }

        /**
         * 攻撃の全シーケンスを取得
         * [ 噴式基地航空隊航空戦, 噴式航空戦, 基地航空隊航空戦, 航空戦1, 支援艦隊の攻撃, 航空戦2, 開幕, 夜戦, 砲撃戦1, 雷撃, 砲撃戦2, 砲撃戦3 ]
         * 各戦闘がない場合はnullになる
         * @return
         */
        public BattleAtackDto[][] getAtackSequence() {
            return new BattleAtackDto[][] {
                    ((this.airBaseInjection == null) || (this.airBaseInjection.atacks == null)) ? null : this
                            .toArray(this.airBaseInjection.atacks),
                    ((this.airInjection == null) || (this.airInjection.atacks == null)) ? null : this
                            .toArray(this.airInjection.atacks),
                    this.getAirBaseBattlesArray(),
                    ((this.air == null) || (this.air.atacks == null)) ? null : this.toArray(this.air.atacks),
                    this.support == null ? null : this.toArray(this.support),
                    ((this.air2 == null) || (this.air2.atacks == null)) ? null : this.toArray(this.air2.atacks),
                    this.openingTaisen == null ? null : this.toArray(this.openingTaisen),
                    this.opening == null ? null : this.toArray(this.opening),
                    this.hougeki == null ? null : this.toArray(this.hougeki),
                    this.hougeki1 == null ? null : this.toArray(this.hougeki1),
                    this.raigeki == null ? null : this.toArray(this.raigeki),
                    this.hougeki2 == null ? null : this.toArray(this.hougeki2),
                    this.hougeki3 == null ? null : this.toArray(this.hougeki3),
            };
        }

        /**
         * 戦闘ランクの計算に使われた情報の概要を取得
         * @param battle
         * @return
         */
        public String getRankCalcInfo(BattleExDto battle) {
            boolean isCombined = (this.nowFriendHpCombined != null);
            int numFships = this.nowFriendHp.length;
            int numFshipsCombined = isCombined ? this.nowFriendHpCombined.length : 0;
            int numEships = this.nowEnemyHp.length;
            // 戦闘後に残っている艦数
            int friendNowShips = 0;
            int enemyNowShips = 0;
            // 総ダメージ
            int friendGauge = 0;
            int enemyGauge = 0;

            for (int i = 0; i < numFships; i++) {
                if (this.nowFriendHp[i] > 0) {
                    ++friendNowShips;
                }
                friendGauge += battle.getStartFriendHp()[i] - this.nowFriendHp[i];

            }
            if (isCombined) {
                for (int i = 0; i < numFshipsCombined; i++) {
                    if (this.nowFriendHpCombined[i] > 0) {
                        ++friendNowShips;
                    }
                    friendGauge += battle.getStartFriendHpCombined()[i] - this.nowFriendHpCombined[i];
                }
            }
            for (int i = 0; i < numEships; i++) {
                if (this.nowEnemyHp[i] > 0)
                    ++enemyNowShips;

                enemyGauge += battle.getStartEnemyHp()[i] - this.nowEnemyHp[i];
            }

            //double enemyGaugeRate = (double) enemyGauge / this.enemyGaugeMax;
            //double friendGaugeRate = (double) friendGauge / this.friendGaugeMax;

            return "味方[艦:" + (numFships + numFshipsCombined) + "→" + friendNowShips + " ゲージ:" +
                    friendGauge + "/" + battle.getFriendGaugeMax() +
                    "] 敵[艦:" + this.nowEnemyHp.length + "→" + enemyNowShips + " ゲージ:" + enemyGauge + "/"
                    + battle.getEnemyGaugeMax() +
                    "]" +
                    //"(" + (enemyGaugeRate / friendGaugeRate) + "/" + (friendGaugeRate / enemyGaugeRate) + ") " +
                    "判定:" + this.estimatedRank.rank();
        }

        /**
         * 受け取ったJSON
         * @return
         */
        public JsonObject getJson() {
            if (this.json == null) {
                return null;
            }
            return JsonUtils.fromString(this.json);
        }

        /**
         * この戦闘フェーズのAPIリクエスト先
         * @return
         */
        public String getApi() {
            return this.kind.getApi().getApiName();
        }

        /**
         * この戦闘フェーズの種別
         * @return kind
         */
        public BattlePhaseKind getKind() {
            return this.kind;
        }

        /**
         * この戦闘フェーズ後の味方艦HP（連合艦隊の時は第一艦隊）
         * @return nowFriendHp
         */
        public int[] getNowFriendHp() {
            return this.nowFriendHp;
        }

        /**
         * この戦闘フェーズ後の味方艦HP（連合艦隊でないときはnull）
         * @return nowFriendHpCombined
         */
        public int[] getNowFriendHpCombined() {
            return this.nowFriendHpCombined;
        }

        /**
         * この戦闘フェーズ後の敵艦HP（敵連合艦隊の時は第一艦隊）
         * @return nowEnemyHp
         */
        public int[] getNowEnemyHp() {
            return this.nowEnemyHp;
        }

        /**
         * この戦闘フェーズ後の敵艦HP（敵連合艦隊でないときはnull）
         * @return nowEnemyHp
         */
        public int[] getNowEnemyHpCombined() {
            return this.nowEnemyHpCombined;
        }

        /**
         * この戦闘フェーズ後のランク（予測値）
         * @return estimatedRank
         */
        public ResultRank getEstimatedRank() {
            return this.estimatedRank;
        }

        /**
         * この戦闘フェーズが夜戦か？
         * @return isNight
         */
        public boolean isNight() {
            return this.isNight;
        }

        /**
         * 敵は連合艦隊第二艦隊か？
         * @return isEnemySecond
         */
        public boolean isEnemySecond() {
            return this.isEnemySecond;
        }

        /**
         * 支援攻撃のタイプ
         * @return supportType
         */
        public String getSupportType() {
            return this.supportType;
        }

        /**
         * 触接機 [味方・敵] -1の場合は「触接なし」
         * @return touchPlane
         */
        public int[] getTouchPlane() {
            return this.touchPlane;
        }

        /**
         * 制空状態
         * @return seiku
         */
        public String getSeiku() {
            return this.seiku;
        }

        /**
         * 損害率 [味方, 敵]
         * @return damageRate
         */
        public double[] getDamageRate() {
            return this.damageRate;
        }

        /**
         * @return airInjection
         */
        public AirBattleDto getAirInjection() {
            return this.airInjection;
        }

        /**
         * 航空戦1
         * @return air
         */
        public AirBattleDto getAir() {
            return this.air;
        }

        /**
         * 航空戦2
         * @return air2
         */
        public AirBattleDto getAir2() {
            return this.air2;
        }

        /**
         * 支援艦隊の攻撃
         * @return support
         */
        public List<BattleAtackDto> getSupport() {
            return this.support;
        }


        /**
         * 開幕対潜
         * @return openingTaisen
         */
        public List<BattleAtackDto> getOpeningTaisen() {
            return this.openingTaisen;
        }

        /**
         * 開幕
         * @return opening
         */
        public List<BattleAtackDto> getOpening() {
            return this.opening;
        }

        /**
         * 雷撃戦
         * @return raigeki
         */
        public List<BattleAtackDto> getRaigeki() {
            return this.raigeki;
        }

        /**
         * 夜戦
         * @return hougeki
         */
        public List<BattleAtackDto> getHougeki() {
            return this.hougeki;
        }

        /**
         * 砲撃戦1
         * @return hougeki1
         */
        public List<BattleAtackDto> getHougeki1() {
            return this.hougeki1;
        }

        /**
         * 砲撃戦2
         * @return hougeki2
         */
        public List<BattleAtackDto> getHougeki2() {
            return this.hougeki2;
        }

        /**
         * 砲撃戦3
         * @return hougeki3
         */
        public List<BattleAtackDto> getHougeki3() {
            return this.hougeki3;
        }

        /**
         * @param touchPlane セットする touchPlane
         */
        public void setTouchPlane(int[] touchPlane) {
            this.touchPlane = touchPlane;
        }

        /**
         * @return flarePos
         */
        public int[] getFlarePos() {
            return this.flarePos;
        }

        /**
         * @return airBaseInjection
         */
        public AirBattleDto getAirBaseInjection() {
            return this.airBaseInjection;
        }

        /**
         * @return airBase
         */
        public List<AirBattleDto> getAirBase() {
            return this.airBase;
        }
    }

    /**
     * 戦闘データオブジェクト作成
     * @param date 戦闘のあった日時
     */
    public BattleExDto(Date date) {
        this.battleDate = date;
    }

    /**
     * 母港情報を設定
     * @param shipSpace
     * @param itemSpace
     */
    public void setBasicInfo(int shipSpace, int itemSpace) {
        this.shipSpace = shipSpace;
        this.itemSpace = itemSpace;
    }

    /**
     * 中に保存してあるJSONを使ってフィールドを更新する
     */
    public void readFromJson() {
        // 後から追加したフィールドはnullになっているので最低限のオブジェクトを作成する
        if (this.enemyCombined == null)
            this.enemyCombined = new ArrayList<>();

        if (this.exVersion >= 2) {
            // Jsonが保存してあるのはバージョン2以降
            Phase[] phaseCopy = this.phaseList.toArray(new Phase[0]);
            this.enemy.clear();
            this.enemyCombined.clear();
            this.phaseList.clear();
            for (Phase phase : phaseCopy) {
                //this.addPhase(phase.getJson(), phase.getKind());
                String json = phase.json;
                LinkedTreeMap tree = getGson().fromJson(json,LinkedTreeMap.class);
                this.addPhase2(tree,json,phase.getKind());
            }
            //this.readResultJson(JsonUtils.fromString(this.resultJson));
            this.readResultJson2(getGson().fromJson(this.resultJson,LinkedTreeMap.class));
        }
        else {
            // 旧バージョンのログに対応
            // ドロップの"アイテム"をdropItemNameに移動させる
            if (this.dropItem && !this.dropShip && StringUtils.isEmpty(this.dropItemName)) {
                this.dropItemName = this.dropName;
                this.dropName = "";
                this.dropType = "";
            }
        }
    }

    /**
     * 戦闘フェーズ結果を読み込む
     * @param object 受け取ったJSON
     * @param kind 戦闘の種別
     * @return 作成されたPhaseオブジェクト
     */
    public Phase addPhase(JsonObject object, BattlePhaseKind kind) {
        if (this.phaseList.size() == 0) {
            // 最初のフェーズ
            String dockId;

            if (object.containsKey("api_dock_id")) {
                dockId = object.get("api_dock_id").toString();
            }
            else {
                dockId = object.get("api_deck_id").toString();
            }

            JsonArray nowhps = object.getJsonArray("api_nowhps");
            JsonArray maxhps = object.getJsonArray("api_maxhps");
            JsonArray nowhpsCombined = object.getJsonArray("api_nowhps_combined");
            JsonArray maxhpsCombined = object.getJsonArray("api_maxhps_combined");
            boolean isFriendCombined = object.containsKey("api_fParam_combined");
            boolean isEnemyCombined = object.containsKey("api_eParam_combined");

            int numFships = 6;
            int numFshipsCombined = 0;

            for (int i = 1; i <= 6; ++i) {
                if (maxhps.getInt(i) == -1) {
                    numFships = i - 1;
                    break;
                }
            }
            if (isFriendCombined) {
                numFshipsCombined = 6;
                for (int i = 1; i <= 6; ++i) {
                    if (maxhpsCombined.getInt(i) == -1) {
                        numFshipsCombined = i - 1;
                        break;
                    }
                }
            }

            if (this.friends.size() == 0) { // 再読み込みの場合はスキップ
                this.friends.add(GlobalContext.getDock(dockId));
                if (numFshipsCombined > 0) {
                    this.friends.add(GlobalContext.getDock("2"));
                }
            }

            JsonArray shipKe = object.getJsonArray("api_ship_ke");
            JsonArray eSlots = object.getJsonArray("api_eSlot");
            JsonArray eParams = object.getJsonArray("api_eParam");
            JsonArray eLevel = object.getJsonArray("api_ship_lv");
            for (int i = 1; i < shipKe.size(); i++) {
                int id = shipKe.getInt(i);
                if (id != -1) {
                    int[] slot = JsonUtils.toIntArray(eSlots.getJsonArray(i - 1));
                    int[] param = JsonUtils.toIntArray(eParams.getJsonArray(i - 1));
                    this.enemy.add(new EnemyShipDto(id, slot, param, eLevel.getInt(i)));
                }
            }
            if (isEnemyCombined) {
                JsonArray shipKeCombined = object.getJsonArray("api_ship_ke_combined");
                JsonArray eSlotsCombined = object.getJsonArray("api_eSlot_combined");
                JsonArray eParamsCombined = object.getJsonArray("api_eParam_combined");
                JsonArray eLevelCombined = object.getJsonArray("api_ship_lv_combined");
                for (int i = 1; i < shipKeCombined.size(); i++) {
                    int id = shipKeCombined.getInt(i);
                    if (id != -1) {
                        int[] slot = JsonUtils.toIntArray(eSlotsCombined.getJsonArray(i - 1));
                        int[] param = JsonUtils.toIntArray(eParamsCombined.getJsonArray(i - 1));
                        this.enemyCombined.add(new EnemyShipDto(id, slot, param, eLevelCombined.getInt(i)));
                    }
                }
            }
            int numEships = this.enemy.size();
            int numEshipsCombined = this.enemyCombined.size();

            this.startFriendHp = new int[numFships];
            this.startEnemyHp = new int[numEships];
            this.maxFriendHp = new int[numFships];
            this.maxEnemyHp = new int[numEships];
            if (isFriendCombined) {
                this.startFriendHpCombined = new int[numFshipsCombined];
                this.maxFriendHpCombined = new int[numFshipsCombined];
            }
            else {
                this.maxFriendHpCombined = null;
            }
            if (isEnemyCombined) {
                this.startEnemyHpCombined = new int[numEshipsCombined];
                this.maxEnemyHpCombined = new int[numEshipsCombined];
            }
            else {
                this.maxEnemyHpCombined = null;
            }

            // 陣形
            if (object.containsKey("api_formation")) {
                JsonArray formation = object.getJsonArray("api_formation");
                for (int i = 0; i < 2; ++i) {
                    switch (formation.get(i).getValueType()) {
                    case NUMBER:
                        this.formation[i] = toFormation(formation.getInt(i));
                        break;
                    default:
                        this.formation[i] = toFormation(Integer.parseInt(formation.getString(i)));
                    }
                }
                this.formationMatch = toMatch(formation.getInt(2));
            }

            // 索敵
            JsonArray jsonSearch = object.getJsonArray("api_search");
            if (jsonSearch != null) {
                this.sakuteki = new String[] {
                        toSearch(jsonSearch.getInt(0)),
                        toSearch(jsonSearch.getInt(1))
                };
            }

            // この戦闘の開始前HPを取得
            for (int i = 1; i < nowhps.size(); i++) {
                int hp = nowhps.getInt(i);
                int maxHp = maxhps.getInt(i);
                if (i <= 6) {
                    if (i <= numFships) {
                        this.maxFriendHp[i - 1] = maxHp;
                        this.friendGaugeMax += this.startFriendHp[i - 1] = hp;
                    }
                }
                else {
                    if ((i - 6) <= numEships) {
                        this.maxEnemyHp[i - 1 - 6] = maxHp;
                        this.enemyGaugeMax += this.startEnemyHp[i - 1 - 6] = hp;
                    }
                }
            }
            if (isFriendCombined || isEnemyCombined) {
                for (int i = 1; i < nowhpsCombined.size(); i++) {
                    int hp = nowhpsCombined.getInt(i);
                    int maxHp = maxhpsCombined.getInt(i);
                    if (i <= 6) {
                        if (i <= numFshipsCombined) {
                            this.maxFriendHpCombined[i - 1] = maxHp;
                            this.friendGaugeMax += this.startFriendHpCombined[i - 1] = hp;
                        }
                    }
                    else {
                        if ((i - 6) <= numEshipsCombined) {
                            this.maxEnemyHpCombined[i - 1 - 6] = maxHp;
                            this.enemyGaugeMax += this.startEnemyHpCombined[i - 1 - 6] = hp;
                        }
                    }
                }
            }

            if (isFriendCombined) {
                // 退避
                this.escaped = new boolean[12];
                if (JsonUtils.hasKey(object, "api_escape_idx")) {
                    for (JsonValue jsonShip : object.getJsonArray("api_escape_idx")) {
                        this.escaped[((JsonNumber) jsonShip).intValue() - 1] = true;
                    }
                }
                if (JsonUtils.hasKey(object, "api_escape_idx_combined")) {
                    for (JsonValue jsonShip : object.getJsonArray("api_escape_idx_combined")) {
                        this.escaped[(((JsonNumber) jsonShip).intValue() - 1) + 6] = true;
                    }
                }
                for (int i = 0; i < 2; ++i) {
                    DockDto dock = this.friends.get(i);
                    if (dock != null) {
                        dock.setEscaped(Arrays.copyOfRange(this.escaped, i * 6, (i + 1) * 6));
                    }
                }
            }
        }

        if (this.phaseList.size() > 0) {
            Phase phase = this.phaseList.get(0);
            this.completeDamageAndAddPhase(
                    new Phase(this, object, kind,
                            phase.getNowFriendHp(), phase.getNowFriendHpCombined(),
                            phase.getNowEnemyHp(), phase.getNowEnemyHpCombined()),
                    kind);
        }
        else {
            this.completeDamageAndAddPhase(new Phase(this, object, kind,
                    this.startFriendHp, this.startFriendHpCombined,
                    this.startEnemyHp, this.startEnemyHpCombined), kind);
        }
        return this.phaseList.get(this.phaseList.size() - 1);
    }

    /**
     * 戦闘フェーズ結果を読み込む
     * @param tree 受け取ったJSON
     * @param kind 戦闘の種別
     * @return 作成されたPhaseオブジェクト
     */
    public Phase addPhase2(LinkedTreeMap tree,String json,BattlePhaseKind kind) {
        if (this.phaseList.size() == 0) {
            // 最初のフェーズ
            String dockId;
            if (tree.containsKey("api_dock_id")) {
                dockId = GsonUtil.toIntString(tree.get("api_dock_id"));
            } else {
                dockId = GsonUtil.toIntString(tree.get("api_deck_id"));
            }

            int[] nowhps = GsonUtil.toIntArray(tree.get("api_nowhps"));
            int[] maxhps = GsonUtil.toIntArray(tree.get("api_maxhps"));
            int[] nowhpsCombined = GsonUtil.toIntArray(tree.get("api_nowhps_combined"));
            int[] maxhpsCombined = GsonUtil.toIntArray(tree.get("api_maxhps_combined"));
            boolean isFriendCombined = tree.containsKey("api_fParam_combined");
            boolean isEnemyCombined = tree.containsKey("api_eParam_combined");

            int numFships = 6;
            int numFshipsCombined = 0;

            for (int i = 1; i <= 6; ++i) {
                if (maxhps[i] == -1) {
                    numFships = i-1;
                    break;
                }
            }
            if (tree.containsKey("api_fParam_combined")) {
                numFshipsCombined = 6;
                for (int i = 1; i <= 6; ++i) {
                    if (maxhpsCombined[i] == -1) {
                        numFshipsCombined = i - 1;
                        break;
                    }
                }
            }

            if (this.friends.size() == 0) { // 再読み込みの場合はスキップ
                this.friends.add(GlobalContext.getDock(dockId));
                if (numFshipsCombined > 0) {
                    this.friends.add(GlobalContext.getDock("2"));
                }
            }

            int[] shipKe = GsonUtil.toIntArray(tree.get("api_ship_ke"));
            int[][] eSlots = GsonUtil.toIntArrayArray(tree.get("api_eSlot"));
            int[][] eParams = GsonUtil.toIntArrayArray(tree.get("api_eParam"));
            int[] eLevel = GsonUtil.toIntArray(tree.get("api_ship_lv"));
            for (int i = 1; i < shipKe.length; i++) {
                int id = shipKe[i];
                if (id != -1) {
                    int[] slot = eSlots[i - 1];
                    int[] param = eParams[i - 1];
                    this.enemy.add(new EnemyShipDto(id, slot, param, eLevel[i]));
                }
            }
            if (isEnemyCombined) {
                int[] shipKeCombined = GsonUtil.toIntArray(tree.get("api_ship_ke_combined"));
                int[][] eSlotsCombined = GsonUtil.toIntArrayArray(tree.get("api_eSlot_combined"));
                int[][] eParamsCombined = GsonUtil.toIntArrayArray(tree.get("api_eParam_combined"));
                int[] eLevelCombined = GsonUtil.toIntArray(tree.get("api_ship_lv_combined"));
                for (int i = 1; i < shipKeCombined.length; i++) {
                    int id = shipKeCombined[i];
                    if (id != -1) {
                        int[] slot = eSlotsCombined[i - 1];
                        int[] param = eParamsCombined[i - 1];
                        this.enemyCombined.add(new EnemyShipDto(id, slot, param, eLevelCombined[i]));
                    }
                }
            }
            int numEships = this.enemy.size();
            int numEshipsCombined = this.enemyCombined.size();

            this.startFriendHp = new int[numFships];
            this.startEnemyHp = new int[numEships];
            this.maxFriendHp = new int[numFships];
            this.maxEnemyHp = new int[numEships];
            if (isFriendCombined) {
                this.startFriendHpCombined = new int[numFshipsCombined];
                this.maxFriendHpCombined = new int[numFshipsCombined];
            }
            else {
                this.maxFriendHpCombined = null;
            }
            if (isEnemyCombined) {
                this.startEnemyHpCombined = new int[numEshipsCombined];
                this.maxEnemyHpCombined = new int[numEshipsCombined];
            }
            else {
                this.maxEnemyHpCombined = null;
            }

            // 陣形
            if (tree.containsKey("api_formation")) {
                int[] formation = GsonUtil.toIntArray(tree.get("api_formation"));
                for (int i = 0; i < 2; ++i) {
                    this.formation[i] = toFormation(formation[i]);
                }
                this.formationMatch = toMatch(formation[2]);
            }

            // 索敵
            int[] jsonSearch = GsonUtil.toIntArray(tree.get("api_search"));
            if (jsonSearch != null) {
                this.sakuteki = new String[] {
                        toSearch(jsonSearch[0]),
                        toSearch(jsonSearch[1])
                };
            }

            // この戦闘の開始前HPを取得
            for (int i = 1; i < nowhps.length; i++) {
                int hp = nowhps[i];
                int maxHp = maxhps[i];
                if (i <= 6) {
                    if (i <= numFships) {
                        this.maxFriendHp[i - 1] = maxHp;
                        this.friendGaugeMax += this.startFriendHp[i - 1] = hp;
                    }
                } else {
                    if ((i - 6) <= numEships) {
                        this.maxEnemyHp[i - 1 - 6] = maxHp;
                        this.enemyGaugeMax += this.startEnemyHp[i - 1 - 6] = hp;
                    }
                }
            }
            if (isFriendCombined || isEnemyCombined) {
                for (int i = 1; i < nowhpsCombined.length; i++) {
                    int hp = nowhpsCombined[i];
                    int maxHp = maxhpsCombined[i];
                    if (i <= 6) {
                        if (i <= numFshipsCombined) {
                            this.maxFriendHpCombined[i - 1] = maxHp;
                            this.friendGaugeMax += this.startFriendHpCombined[i - 1] = hp;
                        }
                    } else {
                        if ((i - 6) <= numEshipsCombined) {
                            this.maxEnemyHpCombined[i - 1 - 6] = maxHp;
                            this.enemyGaugeMax += this.startEnemyHpCombined[i - 1 - 6] = hp;
                        }
                    }
                }
            }
            if (isFriendCombined) {
                // 退避
                this.escaped = new boolean[12];
                if (tree.containsKey("api_escape_idx")) {
                    for (int jsonShip : GsonUtil.toIntArray(tree.get("api_escape_idx"))) {
                        this.escaped[jsonShip - 1] = true;
                    }
                }
                if (tree.containsKey("api_escape_idx_combined")) {
                    for (int jsonShip : GsonUtil.toIntArray(tree.get("api_escape_idx_combined"))) {
                        this.escaped[jsonShip - 1 + 6] = true;
                    }
                }
                for (int i = 0; i < 2; ++i) {
                    this.friends.get(i).setEscaped(Arrays.copyOfRange(this.escaped, i * 6, (i + 1) * 6));
                }
            }
        }

        if (this.phaseList.size() > 0) {
            Phase phase = this.phaseList.get(0);
            this.completeDamageAndAddPhase(new Phase(this, tree, json, kind,
                    phase.getNowFriendHp(), phase.getNowFriendHpCombined(),
                    phase.getNowEnemyHp(), phase.getNowEnemyHpCombined()), kind);
        }
        else {
            this.completeDamageAndAddPhase(new Phase(this, tree, json, kind,
                    this.startFriendHp, this.startFriendHpCombined,
                    this.startEnemyHp, this.startEnemyHpCombined), kind);
        }
        return this.phaseList.get(this.phaseList.size() - 1);
    }


    private void completeDamageAndAddPhase(Phase phase, BattlePhaseKind kind) {
        if (kind.isPractice()) {
            phase.practiceDamage(this);
        }
        else {
            phase.battleDamage(this);
        }
        this.phaseList.add(phase);
    }

    private void readResultJson(JsonObject object) {
        if (object.get("api_quest_name") != null) {
            this.questName = object.getString("api_quest_name");
        }
        else {
            // 演習の場合はない
            this.questName = null;
        }
        this.rank = ResultRank.fromRank(object.getString("api_win_rank"));
        // 完全勝利Sは分からないので戦闘結果を見る
        Phase lastPhase = this.getLastPhase();
        if ((lastPhase != null) && (lastPhase.getEstimatedRank() == ResultRank.PERFECT)) {
            this.rank = ResultRank.PERFECT;
        }
        this.enemyName = object.getJsonObject("api_enemy_info").getString("api_deck_name");
        this.dropShip = object.containsKey("api_get_ship");
        this.dropItem = object.containsKey("api_get_useitem");
        if (this.dropShip) {
            JsonObject getShip = object.getJsonObject("api_get_ship");
            this.dropShipId = getShip.getInt("api_ship_id");
            this.dropType = getShip.getString("api_ship_type");
            this.dropName = getShip.getString("api_ship_name");
        }
        else {
            this.dropType = "";
            this.dropName = "";
        }
        if (this.dropItem) {
            String name = UseItem.get(object.getJsonObject("api_get_useitem").getInt("api_useitem_id"));
            this.dropItemName = StringUtils.defaultString(name);
        }
        else {
            this.dropItemName = "";
        }
        this.mvp = object.getInt("api_mvp");
        if (JsonUtils.hasKey(object, "api_mvp_combined")) {
            this.mvpCombined = object.getInt("api_mvp_combined");
        }
        this.hqLv = object.getInt("api_member_lv");
        if (JsonUtils.hasKey(object, "api_escape")) {
            JsonObject jsonEscape = object.getJsonObject("api_escape");
            this.escapeInfo = new int[] {
                    jsonEscape.getJsonArray("api_escape_idx").getInt(0) - 1,
                    jsonEscape.getJsonArray("api_tow_idx").getInt(0) - 1
            };
        }
        if (JsonUtils.hasKey(object, "api_lost_flag")) {
            this.lostflag = new boolean[6];
            JsonArray jsonLostflag = object.getJsonArray("api_lost_flag");
            for (int i = 1; i < jsonLostflag.size(); i++) {
                this.lostflag[i - 1] = (jsonLostflag.getInt(i) != 0);
            }
        }
    }


    private void readResultJson2(LinkedTreeMap tree) {
        Object questName = tree.get("api_quest_name");
        if (questName instanceof String) {
            this.questName = (String)questName;
        }
        else {
            // 演習の場合はない
            this.questName = null;
        }
        this.rank = ResultRank.fromRank(tree.get("api_win_rank").toString());
        // 完全勝利Sは分からないので戦闘結果を見る
        Phase lastPhase = this.getLastPhase();
        if ((lastPhase != null) && (lastPhase.getEstimatedRank() == ResultRank.PERFECT)) {
            this.rank = ResultRank.PERFECT;
        }
        this.enemyName = ((LinkedTreeMap)tree.get("api_enemy_info")).get("api_deck_name").toString();
        this.dropShip = tree.containsKey("api_get_ship");
        this.dropItem = tree.containsKey("api_get_useitem");
        if (this.dropShip) {
            LinkedTreeMap getShip = (LinkedTreeMap)tree.get("api_get_ship");
            this.dropShipId = GsonUtil.toInt(getShip.get("api_ship_id"));
            this.dropType = getShip.get("api_ship_type").toString();
            this.dropName = getShip.get("api_ship_name").toString();
        }
        else {
            this.dropType = "";
            this.dropName = "";
        }
        if (this.dropItem) {
            String name = UseItem.get(GsonUtil.toInt(((LinkedTreeMap)tree.get("api_get_useitem")).get("api_useitem_id")));
            this.dropItemName = StringUtils.defaultString(name);
        } else {
            this.dropItemName = "";
        }
        this.mvp = GsonUtil.toInt(tree.get("api_mvp"));
        if (tree.get("api_mvp_combined")!=null) {
            this.mvpCombined = GsonUtil.toInt(tree.get("api_mvp_combined"));
        }
        this.hqLv = GsonUtil.toInt(tree.get("api_member_lv"));
        if (tree.get("api_escape")!=null) {
            LinkedTreeMap escape = (LinkedTreeMap)tree.get("api_escape");
            this.escapeInfo = new int[] {
                    GsonUtil.toIntArray(escape.get("api_escape_idx"))[0] - 1,
                    GsonUtil.toIntArray(escape.get("api_tow_idx"))[0] - 1
            };
        }
        if (tree.get("api_lost_flag")!=null) {
            this.lostflag = new boolean[6];
            int[] lostflagArray = GsonUtil.toIntArray(tree.get("api_lost_flag"));
            for (int i = 1; i < lostflagArray.length; i++) {
                this.lostflag[i - 1] = (lostflagArray[i] != 0);
            }
        }
    }

    /**
     * 戦闘結果を読み込む
     * @param object 受け取ったJSON
     * @param mapInfo マス情報
     */
    public void setResult(JsonObject object, MapCellDto mapInfo) {
        this.resultJson = object.toString();
        this.mapCellDto = mapInfo;
        this.readResultJson(object);
    }

    public static String toFormation(int f) {
        String formation;
        switch (f) {
        case 1:
            formation = "単縦陣";
            break;
        case 2:
            formation = "複縦陣";
            break;
        case 3:
            formation = "輪形陣";
            break;
        case 4:
            formation = "梯形陣";
            break;
        case 5:
            formation = "単横陣";
            break;
        case 11:
            formation = "第一警戒航行序列";
            break;
        case 12:
            formation = "第二警戒航行序列";
            break;
        case 13:
            formation = "第三警戒航行序列";
            break;
        case 14:
            formation = "第四警戒航行序列";
            break;
        default:
            formation = "単縦陣";
            break;
        }
        return formation;
    }

    public static int fromFormation(String f) {
        if (f.startsWith("単縦"))
            return 1;
        else if (f.startsWith("複縦"))
            return 2;
        else if (f.startsWith("輪形"))
            return 3;
        else if (f.startsWith("梯形"))
            return 4;
        else if (f.startsWith("単横"))
            return 5;
        else if (f.startsWith("第一警戒"))
            return 11;
        else if (f.startsWith("第二警戒"))
            return 12;
        else if (f.startsWith("第三警戒"))
            return 13;
        else if (f.startsWith("第四警戒"))
            return 14;
        else
            return 1;
    }

    public static String toMatch(int id) {
        switch (id) {
        case 1:
            return "同航戦";
        case 2:
            return "反航戦";
        case 3:
            return "Ｔ字有利";
        case 4:
            return "Ｔ字不利";
        default:
            return "不明(" + id + ")";
        }
    }

    public static String toSupport(int id) {
        switch (id) {
        case 1:
            return "航空支援";
        case 2:
            return "支援射撃";
        case 3:
            return "支援長距離雷撃";
        default:
            return "不明(" + id + ")";
        }
    }

    public static String toSearch(int id) {
        switch (id) {
        case 1:
            return "発見!";
        case 2:
            return "発見!索敵機未帰還機あり";
        case 3:
            return "発見できず…索敵機未帰還機あり";
        case 4:
            return "発見できず…";
        case 5:
            return "発見!(索敵機なし)";
        case 6:
            return "なし";
        default:
            return "不明(" + id + ")";
        }
    }

    /**
     * 保存用エネミーデータ作成
     * @param enemyId
     * @param enemyName
     * @return
     */
    public EnemyData getEnemyData(int enemyId, String enemyName) {
        int[] enemyShips = new int[] { -1, -1, -1, -1, -1, -1 };
        for (int i = 0; i < this.enemy.size(); ++i) {
            enemyShips[i] = this.enemy.get(i).getShipId();
        }
        return new EnemyData(enemyId, enemyName, enemyShips, this.formation[1]);
    }

    /**
     * 味方は連合艦隊か？
     * @return
     */
    public boolean isCombined() {
        return (this.startFriendHpCombined != null);
    }

    /**
     * 敵は連合艦隊か？
     * @return
     */
    public boolean isEnemyCombined() {
        return (this.startEnemyHpCombined != null);
    }

    /**
     * 最後に行ったフェーズを取得
     * @return
     */
    public Phase getLastPhase() {
        if (this.phaseList.size() == 0)
            return null;
        return this.phaseList.get(this.phaseList.size() - 1);
    }

    /**
     * 最初のフェーズを取得
     * @return
     */
    public Phase getPhase1() {
        if (this.phaseList.size() < 1)
            return null;
        return this.phaseList.get(0);
    }

    /**
     * ２番目のフェーズ（ない時はnull）
     * @return
     */
    public Phase getPhase2() {
        if (this.phaseList.size() < 2)
            return null;
        return this.phaseList.get(1);
    }

    /**
     * 戦闘結果も含んでいるか
     * これがfalseに場合は正常に記録されない
     * @return
     */
    public boolean isCompleteResult() {
        if (this.questName != null) {
            // 出撃の場合
            if (this.mapCellDto == null) {
                return false;
            }
        }
        else {
            // 演習の場合
        }
        return (this.friends != null) && (this.getDock() != null) &&
                (this.rank != null) && (this.phaseList != null) &&
                (this.phaseList.size() > 0);
    }

    /**
     * 演習か？
     * @return
     */
    public boolean isPractice() {
        return (this.questName == null);
    }

    /**
     * 交戦後の味方艦HP（連合艦隊の時は第一艦隊）
     * @return
     */
    public int[] getNowFriendHp() {
        return this.getLastPhase().getNowFriendHp();
    }

    /**
     * 交戦後の味方艦HP（連合艦隊でないときはnull）
     * @return
     */
    public int[] getNowFriendHpCombined() {
        return this.getLastPhase().getNowFriendHpCombined();
    }

    /**
     * 交戦後の敵艦HP
     * @return
     */
    public int[] getNowEnemyHp() {
        return this.getLastPhase().getNowEnemyHp();
    }

    /**
     * 交戦後の敵艦HP 連合艦隊第二艦隊
     * @return
     */
    public int[] getNowEnemyHpCombined() {
        return this.getLastPhase().getNowEnemyHpCombined();
    }

    /**
     * 味方艦隊（連合艦隊の時は第一艦隊）
     * @return
     */
    public DockDto getDock() {
        return this.friends.get(0);
    }

    /**
     * 連合艦隊第二艦隊（連合艦隊でないときはnull）
     * @return
     */
    public DockDto getDockCombined() {
        if (this.friends.size() < 2)
            return null;
        return this.friends.get(1);
    }

    /**
     * 戦闘のあった日時
     * @return battleDate
     */
    public Date getBattleDate() {
        return this.battleDate;
    }

    /**
     * 味方艦隊
     * @return friends
     */
    public List<DockDto> getFriends() {
        return this.friends;
    }

    /**
     * 敵艦
     * @return enemy
     */
    public List<EnemyShipDto> getEnemy() {
        return this.enemy;
    }

    /**
     * 敵艦 連合艦隊第二艦隊
     * @return enemy
     */
    public List<EnemyShipDto> getEnemyCombined() {
        return this.enemyCombined;
    }

    /**
     * 味方艦のMaxHP
     * 連合艦隊の時は第一艦隊のみ
     * @return maxFriendHp
     */
    public int[] getMaxFriendHp() {
        return this.maxFriendHp;
    }

    /**
     * 味方連合艦隊第二艦隊のMaxHP
     * @return maxFriendHpCombined
     */
    public int[] getMaxFriendHpCombined() {
        return this.maxFriendHpCombined;
    }

    /**
     * 敵艦のMaxHP
     * @return maxEnemyHp
     */
    public int[] getMaxEnemyHp() {
        return this.maxEnemyHp;
    }

    /**
     * 敵連合艦隊第二艦隊のMaxHP
     * @return maxEnemyHpCombined
     */
    public int[] getMaxEnemyHpCombined() {
        return this.maxEnemyHpCombined;
    }

    /**
     * 戦闘開始時の味方艦のHP
     * 連合艦隊の時は第一艦隊のみ
     * @return startFriendHp
     */
    public int[] getStartFriendHp() {
        return this.startFriendHp;
    }

    /**
     * 味方連合艦隊第二艦隊の戦闘開始時HP
     * @return startFriendHpCombined
     */
    public int[] getStartFriendHpCombined() {
        return this.startFriendHpCombined;
    }

    /**
     * 敵艦の戦闘開始時HP
     * @return startEnemyHp
     */
    public int[] getStartEnemyHp() {
        return this.startEnemyHp;
    }

    /**
     * 敵艦の戦闘開始時HP 連合艦隊第二艦隊
     * @return startEnemyHpCombined
     */
    public int[] getStartEnemyHpCombined() {
        return this.startEnemyHpCombined;
    }

    /**
     * 味方戦果ゲージの最大（味方艦MaxHPの合計）
     * @return friendGaugeMax
     */
    public int getFriendGaugeMax() {
        return this.friendGaugeMax;
    }

    /**
     * 敵戦果ゲージの最大（敵艦MaxHPの合計）
     * @return enemyGaugeMax
     */
    public int getEnemyGaugeMax() {
        return this.enemyGaugeMax;
    }

    /**
     * 陣形 [味方, 敵]
     * @return formation
     */
    public String[] getFormation() {
        return this.formation;
    }

    /**
     * 同航戦、反航戦など
     * @return formationMatch
     */
    public String getFormationMatch() {
        return this.formationMatch;
    }

    /**
     * 索敵状況 [味方, 敵]
     * @return sakuteki
     */
    public String[] getSakuteki() {
        return this.sakuteki;
    }

    /**
     * 出撃海域情報
     * @return questName
     */
    public String getQuestName() {
        return this.questName;
    }

    /**
     * 戦闘結果のランク
     * @return rank
     */
    public ResultRank getRank() {
        return this.rank;
    }

    /**
     * 戦闘のあったマスの情報
     * @return mapCelldto
     */
    public MapCellDto getMapCellDto() {
        return this.mapCellDto;
    }

    /**
     * 敵艦隊の名前
     * @return enemyName
     */
    public String getEnemyName() {
        return this.enemyName;
    }

    /**
     * ドロップ艦があったか？
     * @return dropShip
     */
    public boolean isDropShip() {
        return this.dropShip;
    }

    /**
     * ドロップアイテムがあったか？
     * @return dropItem
     */
    public boolean isDropItem() {
        return this.dropItem;
    }

    /**
     * ドロップ艦の艦種（アイテムの場合は「アイテム」）
     * @return dropType
     */
    public String getDropType() {
        return this.dropType;
    }

    /**
     * ドロップ艦の名前
     * @return dropName
     */
    public String getDropName() {
        return this.dropName;
    }

    /**
     * ドロップアイテムの名前
     * @return dropItemName
     */
    public String getDropItemName() {
        return this.dropItemName;
    }

    /**
     * 戦闘フェーズ（昼戦・夜戦）リスト
     * @return phaseList
     */
    public List<Phase> getPhaseList() {
        return this.phaseList;
    }

    /**
     * MVP艦が何番目の艦か (0～)
     * MVPがいない時は-1
     * @return mvp
     */
    public int getMvp() {
        return this.mvp;
    }

    /**
     * 連合艦隊第二艦隊のMVP艦が何番目の艦か
     * 連合艦隊でない時またはMVPがいない時は-1
     * @return mvpCombined
     */
    public int getMvpCombined() {
        return this.mvpCombined;
    }

    /**
     * 司令部Lv
     * @return hqLv
     */
    public int getHqLv() {
        return this.hqLv;
    }

    /***
     * BattleExDtoのバージョン
     * exVersion == 0 : Tag 34以降がない
     * exVersion == 1 : Tag 36まである
     * exVersion == 2 : Jsonがある
     * @return exVersion
     */
    public int getExVersion() {
        return this.exVersion;
    }

    void setExVersion(int exVersion) {
        this.exVersion = exVersion;
    }

    /**
     * 母港の艦娘空き枠
     * @return shipSpace
     */
    public int getShipSpace() {
        return this.shipSpace;
    }

    /**
     * 母港の装備アイテム空き枠
     * @return itemSpace
     */
    public int getItemSpace() {
        return this.itemSpace;
    }

    /**
     * 連合艦隊における退避意見 [退避する艦(0-11), 護衛艦(0-11)]
     * @return escapeInfo
     */
    public int[] getEscapeInfo() {
        return this.escapeInfo;
    }

    /**
     * 護衛退避で戦線離脱したか [第1艦隊1番艦～第2艦隊6番艦]
     * 艦隊の艦数に関係なく常に長さは12
     * @return escaped
     */
    public boolean[] getEscaped() {
        return this.escaped;
    }

    /**
     * 戦闘結果のレスポンスJSON
     * @return resultJson
     */
    public JsonObject getResultJson() {
        if (this.resultJson == null) {
            return null;
        }
        return JsonUtils.fromString(this.resultJson);
    }

    /**
     * @return lostflag
     */
    public boolean[] getLostflag() {
        return this.lostflag;
    }

    /**
     * @return shipId
     */
    public int getDropShipId() {
        return this.dropShipId;
    }

    //標準装備のスクリプト群については全部Java側で処理することにした
    private static Gson _gson = new Gson();
    private static Gson getGson(){
        return _gson;
    }

    private static ArrayList<String> PhaseRowHeader(){
        ArrayList<String> header = new ArrayList<String>();
        header.add("日付");
        header.add("海域");
        header.add("マス");
        header.add("出撃");
        header.add("ランク");
        header.add("敵艦隊");
        header.add("提督レベル");
        header.add("自陣形");
        header.add("敵陣形");
        return header;
    }
    private ArrayList<String> PhaseRowBody(){
        ArrayList<String> body = new ArrayList<String>();
        body.add("");//日付は最後に追加する
        body.add(this.getQuestName());
        String report = "";
        String reportType = "";
        if(this.mapCellDto != null){
            report = this.mapCellDto.getReportString();
            if(this.mapCellDto.isStart()&&this.mapCellDto.isBoss()){
                reportType = "出撃&ボス";
            }else if(this.mapCellDto.isStart()){
                reportType = "出撃";
            }else if(this.mapCellDto.isBoss()){
                reportType = "ボス";
            }
        }
        body.add(report);
        body.add(reportType);
        body.add(this.rank.toString());
        body.add(this.enemyName);
        body.add((new Integer(this.hqLv)).toString());

        String formation0 = "";
        String formation1 = "";
        if(this.formation != null){
            formation0 = this.formation[0];
            formation1 = this.formation[1];
        }
        body.add(formation0);
        body.add(formation1);
        return body;
    }
    private static ArrayList<String> DayPhaseRowHeader(){
        ArrayList<String> header = PhaseRowHeader();
        header.add("自索敵");
        header.add("敵索敵");
        header.add("制空権");
        header.add("会敵");
        header.add("自触接");
        header.add("敵触接");
        header.add("自照明弾");
        header.add("敵照明弾");
        return header;
    }
    private ArrayList<String> DayPhaseRowBody(){
        ArrayList<String> body = this.PhaseRowBody();
        if(this.sakuteki != null){
            body.add(this.sakuteki[0]);
            body.add(this.sakuteki[1]);
        }else{
            body.add("");
            body.add("");
        }

        Phase phase1 = this.getPhase1();
        String seiku = "";
        if(phase1 != null && phase1.air != null){
            seiku = phase1.air.seiku;
        }
        body.add(seiku);
        body.add(this.formationMatch);

        String touchPlane0 = "";
        String touchPlane1 = "";
        if(phase1 != null && phase1.air != null && phase1.getTouchPlane() != null){
            String[] names = phase1.air.getTouchPlane();
            touchPlane0 = (names[0].equals("なし"))?"" :names[0];
            touchPlane1 = (names[1].equals("なし"))?"" :names[1];
        }
        body.add(touchPlane0);
        body.add(touchPlane1);

        body.add("");
        body.add("");
        return body;
    }
    private ArrayList<String> DayPhaseRowBodyAir(AirBattleDto air){
        ArrayList<String> body = this.PhaseRowBody();
        if(this.sakuteki != null){
            body.add(this.sakuteki[0]);
            body.add(this.sakuteki[1]);
        }else{
            body.add("");
            body.add("");
        }

        Phase phase1 = this.getPhase1();
        String seiku = "";
        if(air != null){
            seiku = air.seiku;
        }
        body.add(seiku);
        body.add(this.formationMatch);

        String touchPlane0 = "";
        String touchPlane1 = "";
        if(air != null){
            String[] names = air.getTouchPlane();
            touchPlane0 = (names[0].equals("なし"))?"" :names[0];
            touchPlane1 = (names[1].equals("なし"))?"" :names[1];
        }
        body.add(touchPlane0);
        body.add(touchPlane1);

        body.add("");
        body.add("");
        return body;
    }
    static private ArrayList<String> NightPhaseRowHeader(){
        return DayPhaseRowHeader();
    }
    private ArrayList<String> NightPhaseRowBody(){
        Phase phase = null;
        if(this.getPhase1()!=null && this.getPhase1().isNight){
            phase = this.getPhase1();
        }else if(this.getPhase2()!=null && this.getPhase2().isNight){
            phase = this.getPhase2();
        }else{
            ArrayList<String> body = this.PhaseRowBody();
            int length = NightPhaseRowHeader().size();
            for(int i=body.size();i<length;i++){
                body.add("");
            }
            return body;
        }
        ArrayList<String> body = this.PhaseRowBody();
        body.add("");
        body.add("");
        body.add("");
        body.add(this.formationMatch);
        String touchPlane0 = "";
        String touchPlane1 = "";
        if(phase.touchPlane != null){
            String[] names = AirBattleDto.toTouchPlaneString(phase.touchPlane);
            touchPlane0 = (names[0].equals("なし"))?"" :names[0];
            touchPlane1 = (names[1].equals("なし"))?"" :names[1];
        }
        body.add(touchPlane0);
        body.add(touchPlane1);
        String flarePos0 = "";
        String flarePos1 = "";
        if(phase.flarePos!=null){
            if(phase.flarePos[0]>0){
                flarePos0 = (new Integer(phase.flarePos[0])).toString();
            }
            if(phase.flarePos[1]>0){
                flarePos1 = (new Integer(phase.flarePos[1])).toString();
            }
        }
        body.add(flarePos0);
        body.add(flarePos1);
        return body;
    }

    private static ArrayList<String> _itemRowHeader = null;
    private static ArrayList<String> ItemRowHeader(){
        if(_itemRowHeader == null){
            ArrayList<String> header = new ArrayList<String>();
            for(int i=1;i<=5;i++){
                header.add(String.format("装備%d.名前",i));
                header.add(String.format("装備%d.改修",i));
                header.add(String.format("装備%d.熟練度",i));
                header.add(String.format("装備%d.搭載数",i));
            }
            _itemRowHeader = header;
        }
        return _itemRowHeader;
    }
    private ArrayList<String> ItemRowBodyConstruct(ItemDto item,ItemInfoDto info,Integer onSlot){
        ArrayList<String> body = new ArrayList<String>();
        String name = "";
        String level = "";
        String alv = "";
        String onSlotS = "";
        if(info != null){
            name = info.getName();
            if(item != null){
                level = String.valueOf(item.getLevel());
                alv = String.valueOf(item.getAlv());
            }
        }
        if(onSlot != null){
            onSlotS = onSlot.toString();
        }
        body.add(name);
        body.add(level);
        body.add(alv);
        body.add(onSlotS);
        return body;
    }
    private ArrayList<String> ItemRowBody(ShipBaseDto ship){
        ArrayList<String> body = new ArrayList<String>();

        List<ItemDto>itemDtos = null;
        ItemDto itemExDto = null;
        List<ItemInfoDto>itemInfoDtos = null;
        ItemInfoDto itemInfoExDto = null;
        int[] onSlots = null;
        if(ship != null){
            if(ship instanceof ShipDto){
                ShipDto s = (ShipDto)ship;
                itemDtos = s.getItem2();
                itemExDto = s.getSlotExItem();
                if(itemExDto != null){
                    itemInfoExDto = itemExDto.getInfo();
                }
            }
            itemInfoDtos = ship.getItem();
            onSlots = ship.getOnSlot();
        }
        for(int i=0;i<5;i++){
            ArrayList<String> itemRow = null;
            if(i==4 && itemExDto != null && itemInfoExDto != null){
                itemRow = this.ItemRowBodyConstruct(itemExDto, itemInfoExDto, null);
            }
            else if(itemInfoDtos != null && i < itemInfoDtos.size()){
                Integer onSlot = null;
                if(onSlots != null && i < onSlots.length){
                    onSlot = Integer.valueOf(onSlots[i]);
                }
                if(itemDtos != null && i < itemDtos.size()){
                    itemRow = this.ItemRowBodyConstruct(itemDtos.get(i),itemInfoDtos.get(i),onSlot);
                }else{
                    itemRow = this.ItemRowBodyConstruct(null, itemInfoDtos.get(i), onSlot);
                }
            }
            else{
                itemRow = this.ItemRowBodyConstruct(null, null, null);
            }
            body.addAll(itemRow);
        }
        return body;
    }

    private static ArrayList<String> _shipRowHeader = null;
    private static ArrayList<String> ShipRowHeader(){
        if(_shipRowHeader != null){
            return _shipRowHeader;
        }
        ArrayList<String> header = new ArrayList<String>();
        header.add("編成順");
        header.add("ID");
        header.add("名前");
        header.add("種別");
        header.add("疲労");
        header.add("残耐久");
        header.add("最大耐久");
        header.add("損傷");
        header.add("残燃料");
        header.add("最大燃料");
        header.add("残弾薬");
        header.add("最大弾薬");
        header.add("Lv");
        header.add("速力");
        header.add("火力");
        header.add("雷装");
        header.add("対空");
        header.add("装甲");
        header.add("回避");
        header.add("対潜");
        header.add("索敵");
        header.add("運");
        header.add("射程");
        header.addAll(ItemRowHeader());
        _shipRowHeader = header;
        return _shipRowHeader;
    }
    //戦闘中に更新されるHPと損小状態が空、女神など非対応
    private ArrayList<String> ShipRowBodyBase(ShipBaseDto ship,int maxHp,int index){
        if(ship != null){
            ArrayList<String> body = new ArrayList<String>();
            String shipId = "";
            String fullName = "";
            String type = "";
            String maxFuel = "";
            String maxBull = "";
            if(ship.shipInfo != null){
                shipId = String.valueOf(ship.shipInfo.getShipId());
                fullName = ship.shipInfo.getFullName();
                type = ship.shipInfo.getType();
                maxFuel = String.valueOf(ship.shipInfo.getMaxFuel());
                maxBull = String.valueOf(ship.shipInfo.getMaxBull());
            }
            String cond = "";
            String fuel = "";
            String bull = "";
            if(ship instanceof ShipDto){
                ShipDto s = (ShipDto)ship;
                cond = String.valueOf(s.getCond());
                fuel = String.valueOf(s.getFuel());
                bull = String.valueOf(s.getBull());
            }
            String soku = "";
            String houg = "";
            String raig = "";
            String tyku = "";
            String souk = "";
            String kaih = "";
            String tais = "";
            String saku = "";
            String luck = "";
            String leng = "";
            if(ship.param != null){
                switch(ship.param.getSoku()){
                    case 0:soku = "陸上";break;
                    case 5:soku = "低速";break;
                    case 10:soku = "高速";break;
                }
                houg = String.valueOf(ship.param.getHoug());
                raig = String.valueOf(ship.param.getRaig());
                tyku = String.valueOf(ship.param.getTyku());
                souk = String.valueOf(ship.param.getSouk());
                kaih = String.valueOf(ship.param.getKaih());
                tais = String.valueOf(ship.param.getTais());
                saku = String.valueOf(ship.param.getSaku());
                luck = String.valueOf(ship.param.getLuck());
                switch(ship.param.getLeng()){
                    case 0:leng = "超短";break;
                    case 1:leng = "短";break;
                    case 2:leng = "中";break;
                    case 3:leng = "長";break;
                    case 4:leng = "超長";break;
                }
            }
            String lv = String.valueOf(ship.getLv());
            body.add(String.valueOf(index+1));
            body.add(shipId);
            body.add(fullName);
            body.add(type);
            body.add(cond);
            body.add("");
            body.add(String.valueOf(maxHp));
            body.add("");
            body.add(fuel);
            body.add(maxFuel);
            body.add(bull);
            body.add(maxBull);
            body.add(lv);
            body.add(soku);
            body.add(houg);
            body.add(raig);
            body.add(tyku);
            body.add(souk);
            body.add(kaih);
            body.add(tais);
            body.add(saku);
            body.add(luck);
            body.add(leng);
            body.addAll(this.ItemRowBody(ship));
            return body;
        }else{
            int length = ShipRowHeader().size();
            ArrayList<String> body = new ArrayList<String>();
            for(int i=0;i<length;i++){
                body.add("");
            }
            return body;
        }
    }
    private ArrayList<String> ShipRowBodyUpdate(ArrayList<String> body,int hp,int maxHp){
        String hpText = "";
        double hpRate = 4.0*(double)hp/(double)maxHp;
        if(hpRate > 3){ hpText = "小破未満"; }
        else if(hpRate > 2){ hpText = "小破"; }
        else if(hpRate > 1){ hpText = "中破"; }
        else if(hpRate > 0){ hpText = "大破"; }
        else{ hpText = "轟沈"; }
        body.set(5, String.valueOf(hp));
        body.set(7, hpText);
        return body;
    }



    private static ArrayList<String> _shipSakutekiRowHeader = null;
    private static ArrayList<String> ShipSakutekiRowHeader(){
        if(_shipSakutekiRowHeader != null){
            return _shipSakutekiRowHeader;
        }
        ArrayList<String> header = new ArrayList<String>();
        header.add("編成順");
        header.add("ID");
        header.add("名前");
        header.add("種別");
        header.add("Lv");
        header.add("装備込み索敵");
        header.add("素索敵");
        header.add("索敵スコア");
        header.addAll(ItemSakutekiRowHeader());
        _shipSakutekiRowHeader = header;
        return _shipSakutekiRowHeader;
    }
    private ArrayList<String> ShipSakutekiRowBodyBase(ShipBaseDto ship,int index){
        if(ship != null){
            ArrayList<String> body = new ArrayList<String>();
            String shipId = "";
            String fullName = "";
            String type = "";
            if(ship.shipInfo != null){
                shipId = String.valueOf(ship.shipInfo.getShipId());
                fullName = ship.shipInfo.getFullName();
                type = ship.shipInfo.getType();
            }
            String saku = "";
            String soSakuteki = "";
            String sakutekiScore = "";
            if(ship.param != null && ship instanceof ShipDto){
                ShipDto s = (ShipDto)ship;
                saku = String.valueOf(ship.param.getSaku());
                soSakuteki = String.valueOf(s.getSakutekiWithoutItem());
                double small = 0.00000000000001;
                double ten = 10000000000.0;
                sakutekiScore = String.format("%.10f",(Math.floor((s.getSakutekiScoreWithoutItem()+small)*ten)+0.1)/ten);
            }
            String lv = String.valueOf(ship.getLv());
            body.add(String.valueOf(index+1));
            body.add(shipId);
            body.add(fullName);
            body.add(type);
            body.add(lv);
            body.add(saku);
            body.add(soSakuteki);
            body.add(sakutekiScore);
            body.addAll(this.ItemSakutekiRowBody(ship));
            return body;
        }else{
            int length = ShipRowHeader().size();
            ArrayList<String> body = new ArrayList<String>();
            for(int i=0;i<length;i++){
                body.add("");
            }
            return body;
        }
    }






    private static ArrayList<String> _itemSakutekiRowHeader = null;
    private static ArrayList<String> ItemSakutekiRowHeader(){
        if(_itemSakutekiRowHeader == null){
            ArrayList<String> header = new ArrayList<String>();
            for(int i=1;i<=5;i++){
                header.add(String.format("装備%d.名前",i));
                header.add(String.format("装備%d.カテゴリ",i));
                header.add(String.format("装備%d.索敵",i));
                header.add(String.format("装備%d.改修",i));
                header.add(String.format("装備%d.改修索敵加算値",i));
                header.add(String.format("装備%d.素索敵スコア",i));
                header.add(String.format("装備%d.改修索敵スコア",i));
                header.add(String.format("装備%d.合算索敵スコア",i));
            }
            _itemSakutekiRowHeader = header;
        }
        return _itemSakutekiRowHeader;
    }

    private ArrayList<String> ItemSakutekiRowBodyConstruct(ItemDto item,ItemInfoDto info,Integer onSlot){
        ArrayList<String> body = new ArrayList<String>();
        String name = "";
        String type = "";
        String sakuteki = "";
        String level = "";
        String kaisyuKasan = "";
        String soSakutekiScore = "";
        String kaisyuSakutekiScore = "";
        String gassanSakutekiScore = "";
        if(info != null && item != null){
            name = item.getName();
            type = item.getTypeName();
            sakuteki =  String.valueOf(item.getParam().getSakuteki());
            int lv = item.getLevel();
            level = String.valueOf(item.getLevel());
            double small = 0.00000000000001;
            double ten = 10000000000.0;
            double kaisyuKasanD = item.getSakutekiKaisyuKeisu() * Math.sqrt(lv);
            kaisyuKasan = String.format("%.10f",(Math.floor((kaisyuKasanD+small)*ten)+0.1)/ten);
            soSakutekiScore = String.format("%.10f",(Math.floor((item.getSakutekiScoreWithoutKaisyu()+small)*ten)+0.1)/ten);
            kaisyuSakutekiScore = String.format("%.10f",(Math.floor((item.getKaisyuSakutekiScore()+small)*ten)+0.1)/ten);
            gassanSakutekiScore = String.format("%.10f",(Math.floor((item.getSakutekiScore()+small)*ten)+0.1)/ten);
        }
        body.add(name);
        body.add(type);
        body.add(sakuteki);
        body.add(level);
        body.add(kaisyuKasan);
        body.add(soSakutekiScore);
        body.add(kaisyuSakutekiScore);
        body.add(gassanSakutekiScore);
        return body;
    }

    private ArrayList<String> ItemSakutekiRowBody(ShipBaseDto ship){
        ArrayList<String> body = new ArrayList<String>();

        List<ItemDto>itemDtos = null;
        ItemDto itemExDto = null;
        List<ItemInfoDto>itemInfoDtos = null;
        ItemInfoDto itemInfoExDto = null;
        int[] onSlots = null;
        if(ship != null){
            if(ship instanceof ShipDto){
                ShipDto s = (ShipDto)ship;
                itemDtos = s.getItem2();
                itemExDto = s.getSlotExItem();
                if(itemExDto != null){
                    itemInfoExDto = itemExDto.getInfo();
                }
            }
            itemInfoDtos = ship.getItem();
            onSlots = ship.getOnSlot();
        }
        for(int i=0;i<5;i++){
            ArrayList<String> itemRow = null;
            if(i==4 && itemExDto != null && itemInfoExDto != null){
                itemRow = this.ItemSakutekiRowBodyConstruct(itemExDto, itemInfoExDto, null);
            }
            else if(itemInfoDtos != null && i < itemInfoDtos.size()){
                Integer onSlot = null;
                if(onSlots != null && i < onSlots.length){
                    onSlot = Integer.valueOf(onSlots[i]);
                }
                if(itemDtos != null && i < itemDtos.size()){
                    itemRow = this.ItemSakutekiRowBodyConstruct(itemDtos.get(i),itemInfoDtos.get(i),onSlot);
                }else{
                    itemRow = this.ItemSakutekiRowBodyConstruct(null, itemInfoDtos.get(i), onSlot);
                }
            }
            else{
                itemRow = this.ItemSakutekiRowBodyConstruct(null, null, null);
            }
            body.addAll(itemRow);
        }
        return body;
    }












    private static ArrayList<String>ShipSummaryRowHeader(){
        ArrayList<String> header = new ArrayList<String>();
        header.add("ID");
        header.add("名前");
        header.add("Lv");
        return header;
    }
    private ArrayList<String>ShipSummaryRowBody(ShipBaseDto ship){
        if(ship != null){
            ArrayList<String> body = new ArrayList<String>();
            String shipId = "";
            String fullName = "";
            if(ship.shipInfo != null){
                shipId = String.valueOf(ship.shipInfo.getShipId());
                fullName = ship.shipInfo.getFullName();
            }
            String lv = (new Integer(ship.getLv())).toString();
            body.add(shipId);
            body.add(fullName);
            body.add(lv);
            return body;
        }
        else{
            int length = ShipSummaryRowHeader().size();
            ArrayList<String> body = new ArrayList<String>();
            for(int i=0;i<length;i++){
                body.add("");
            }
            return body;
        }
    }
    //HPは敵、味方、味方第二
    private int[][] createNextHP(int[][] prevHP,List<BattleAtackDto> attackList){
        if(attackList != null){
            int[] enemy = prevHP[0].clone();
            int[] friend = prevHP[1].clone();
            int[] combined = (this.isCombined()) ?prevHP[2].clone() :null;
            int[] enemyCombined = (this.isEnemyCombined()) ?prevHP[3].clone() :null;
            for(int i=0;i<attackList.size();i++){
                BattleAtackDto attack = attackList.get(i);
                for(int j=0;j<attack.target.length;j++){
                    int t = attack.target[j];
                    int damage = attack.damage[j];
                    if(attack.friendAtack){
                        if(t<6){
                            enemy[t] = Math.max(0, enemy[t]-damage);
                        }else{
                            enemyCombined[t-6] = Math.max(0, enemyCombined[t-6]-damage);
                        }
                    }else{
                        if(t<6){
                            friend[t] = Math.max(0, friend[t]-damage);
                        }else{
                            combined[t-6] = Math.max(0, combined[t-6]-damage);
                        }
                    }
                }
            }
            int [][] result = new int[4][];
            result[0] = enemy;
            result[1] = friend;
            result[2] = combined;
            result[3] = enemyCombined;
            return result;
        }
        else{
            return prevHP;
        }
    }
    private int[][] createNextHPAir(int[][] prevHP,AirBattleDto airBattle){
        if(airBattle!=null){
            return this.createNextHP(prevHP, airBattle.atacks);
        }
        else{
            return prevHP;
        }
    }
    private ArrayList<int[][]>createNextHPAirBase(int[][] prevHP,List<AirBattleDto> baseAirBattleList){
        ArrayList<int[][]>result = new ArrayList<int[][]>();
        int [][] prev = prevHP;
        for(int i=0;i<baseAirBattleList.size();i++){
            prev = this.createNextHPAir(prev, baseAirBattleList.get(i));
            result.add(prev);
        }
        return result;
    }
    private ArrayList<ArrayList<int[][]>> createNextHPHougeki(int[][] prevHP,List<BattleAtackDto> attackList){
        int[] enemy = prevHP[0].clone();
        int[] friend = prevHP[1].clone();
        int[] combined = (this.isCombined())?prevHP[2].clone():null;
        int[] enemyCombined = (this.isEnemyCombined())?prevHP[3].clone():null;
        ArrayList<ArrayList<int[][]>>result = new ArrayList<ArrayList<int[][]>>();
        for(int i=0;i<attackList.size();i++){
            ArrayList<int[][]> array = new ArrayList<int[][]>();

            BattleAtackDto attack = attackList.get(i);
            for(int j=0;j<attack.target.length;j++){
                int[][] next = new int[4][];

                int t = attack.target[j];
                int damage = attack.damage[j];
                if(attack.friendAtack){
                    if(t<6){
                        enemy[t] = Math.max(0, enemy[t]-damage);
                    }else{
                        enemyCombined[t-6] = Math.max(0, enemyCombined[t-6]-damage);
                    }
                }else{
                    if(t<6){
                        friend[t] = Math.max(0, friend[t]-damage);
                    }else{
                        combined[t-6] = Math.max(0, combined[t-6]-damage);
                    }
                }

                next[0] = enemy.clone();
                next[1] = friend.clone();
                next[2] = (this.isCombined())?combined.clone():null;
                next[3] = (this.isEnemyCombined())?enemyCombined.clone():null;
                array.add(next);
            }

            result.add(array);
        }
        return result;
    }
    static public ArrayList<String> HougekiRowHeader(){
        ArrayList<String> header = DayPhaseRowHeader();
        header.add("戦闘種別");
        header.add("自艦隊");
        header.add("巡目");
        header.add("攻撃艦");
        header.add("砲撃種別");
        header.add("表示装備1");
        header.add("表示装備2");
        header.add("表示装備3");
        header.add("クリティカル");
        header.add("ダメージ");
        header.add("かばう");
        ArrayList<String> shipHeader = ShipRowHeader();
        int length = shipHeader.size();
        for(int i=0;i<length;i++){
            header.add(String.format("攻撃艦.%s",shipHeader.get(i)));
        }
        for(int i=0;i<length;i++){
            header.add(String.format("防御艦.%s",shipHeader.get(i)));
        }
        header.add("艦隊種類");
        header.add("敵艦隊種類");
        return header;
    }

    private int[][] HougekiRowBodyConstruct(
            ArrayList<ArrayList<String>>enemyRows,
            ArrayList<ArrayList<String>>friendRows,
            List<BattleAtackDto>attackList,
            LinkedTreeMap api_hougeki,
            boolean isSecond,
            String hougekiCount,
            String combinedFlagString,
            int[][]startHP,
            ArrayList<ArrayList<String>> body,
            BuiltinScriptFilter filter)
        {
            int[][]prevHP = startHP;
            if(attackList != null){
                String fleetName =
                    (!this.isCombined())?"通常艦隊"
                    :(isSecond)?"連合第2艦隊"
                    :"連合第1艦隊";
                ArrayList<ArrayList<int[][]>>hougekiHP = this.createNextHPHougeki(startHP, attackList);

                int[] api_at_list = GsonUtil.toIntArray(api_hougeki.get("api_at_list"));
                int[] api_at_type = GsonUtil.toIntArray(api_hougeki.get("api_at_type"));
                int[][] api_df_list = GsonUtil.toIntArrayArray(api_hougeki.get("api_df_list"));
                int[][] api_si_list = GsonUtil.toIntArrayArray(api_hougeki.get("api_si_list"));
                int[][] api_cl_list = GsonUtil.toIntArrayArray(api_hougeki.get("api_cl_list"));
                double[][] api_damage = GsonUtil.toDoubleArrayArray(api_hougeki.get("api_damage"));
                ArrayList<String> dayPhaseRow = this.DayPhaseRowBody();
                for(int i=1;i<api_at_list.length;++i){
                    int at = api_at_list[i];
                    int atType = api_at_type[i];
                    int[] dfList = api_df_list[i];
                    int[] siList = api_si_list[i];
                    int[] clList = api_cl_list[i];
                    double[] damageList = api_damage[i];

                    String attackFleetName = (at<7)?"自軍" :"敵軍";
                    String[] itemName = new String[3];
                    for(int j=0;j<itemName.length;j++){
                        if(j < siList.length && siList[j] > 0){
                            itemName[j]=Item.get(siList[j]).getName();
                        }else{
                            itemName[j]="";
                        }
                    }

                    for(int j=0;j<dfList.length;++j){
                        int df = dfList[j];
                        int damage = (int)damageList[j];
                        boolean kabau = (damageList[j] - (double)damage) > 0.05;
                        int cl = clList[j];
                        ArrayList<String> row = (ArrayList<String>) dayPhaseRow.clone();
                        row.add("砲撃戦");
                        row.add(fleetName);
                        row.add(hougekiCount);
                        row.add(attackFleetName);
                        row.add(String.valueOf(atType));
                        row.add(itemName[0]);
                        row.add(itemName[1]);
                        row.add(itemName[2]);
                        row.add(String.valueOf(cl));
                        row.add(String.valueOf(damage));
                        row.add(kabau?"1" :"0");
                        if(at < 7){row.addAll(this.ShipRowBodyUpdate(friendRows.get(at-1), prevHP[isSecond?2 :1][at-1],isSecond?this.maxFriendHpCombined[at-1] :this.maxFriendHp[at-1]));}
                        else{row.addAll(this.ShipRowBodyUpdate(enemyRows.get(at-7), prevHP[0][at-7],this.maxEnemyHp[at-7]));}
                        if(df < 7){row.addAll(this.ShipRowBodyUpdate(friendRows.get(df-1), prevHP[isSecond?2 :1][df-1],isSecond?this.maxFriendHpCombined[df-1] :this.maxFriendHp[df-1]));}
                        else{row.addAll(this.ShipRowBodyUpdate(enemyRows.get(df-7), prevHP[0][df-7],this.maxEnemyHp[df-7]));}
                        row.add(combinedFlagString);
                        row.add(isEnemyCombined()?"連合艦隊":"通常艦隊");
                        if(filter.filterHougekiAttackDefence(this, at, df, isSecond) && filter.filterOutput(row)){
                            body.add(row);
                        }
                        if(i-1<hougekiHP.size() && j<hougekiHP.get(i-1).size()){
                            prevHP = hougekiHP.get(i-1).get(j);
                        }
                    }
                }
            }
            return prevHP;
        }
    private int[][] HougekiRowBodyConstructEC(
            ArrayList<ArrayList<String>>enemyRows,
            ArrayList<ArrayList<String>>friendRows,
            ArrayList<ArrayList<String>>enemyCombinedRows,
            ArrayList<ArrayList<String>>friendCombinedRows,
            List<BattleAtackDto>attackList,
            LinkedTreeMap api_hougeki,
            String hougekiCount,
            String combinedFlagString,
            int[][]startHP,
            ArrayList<ArrayList<String>> body,
            BuiltinScriptFilter filter)
        {
            int[][]prevHP = startHP;
            if(attackList != null){
                ArrayList<ArrayList<int[][]>>hougekiHP = this.createNextHPHougeki(startHP, attackList);
                int[] api_at_eflag = GsonUtil.toIntArray(api_hougeki.get("api_at_eflag"));
                int[] api_at_list = GsonUtil.toIntArray(api_hougeki.get("api_at_list"));
                int[] api_at_type = GsonUtil.toIntArray(api_hougeki.get("api_at_type"));
                int[][] api_df_list = GsonUtil.toIntArrayArray(api_hougeki.get("api_df_list"));
                int[][] api_si_list = GsonUtil.toIntArrayArray(api_hougeki.get("api_si_list"));
                int[][] api_cl_list = GsonUtil.toIntArrayArray(api_hougeki.get("api_cl_list"));
                double[][] api_damage = GsonUtil.toDoubleArrayArray(api_hougeki.get("api_damage"));
                ArrayList<String> dayPhaseRow = this.DayPhaseRowBody();
                for(int i=1;i<api_at_list.length;++i){
                    int eflag = api_at_eflag[i];
                    int at = api_at_list[i];
                    int atType = api_at_type[i];
                    int[] dfList = api_df_list[i];
                    int[] siList = api_si_list[i];
                    int[] clList = api_cl_list[i];
                    double[] damageList = api_damage[i];

                    String attackFleetName = (eflag==0)?"自軍" :"敵軍";
                    String[] itemName = new String[3];
                    for(int j=0;j<itemName.length;j++){
                        if(j < siList.length && siList[j] > 0){
                            itemName[j]=Item.get(siList[j]).getName();
                        }else{
                            itemName[j]="";
                        }
                    }

                    for(int j=0;j<dfList.length;++j){
                        int df = dfList[j];
                        int damage = (int)damageList[j];
                        boolean kabau = (damageList[j] - (double)damage) > 0.05;
                        int cl = clList[j];
                        ArrayList<String> row = (ArrayList<String>) dayPhaseRow.clone();
                        String fleetName =
                                (!this.isCombined())?"通常艦隊"
                                :(eflag == 1 && df > 6)||(eflag == 0 && at > 6) ?"連合第2艦隊"
                                :"連合第1艦隊";
                        row.add("砲撃戦");
                        row.add(fleetName);
                        row.add(hougekiCount);
                        row.add(attackFleetName);
                        row.add(String.valueOf(atType));
                        row.add(itemName[0]);
                        row.add(itemName[1]);
                        row.add(itemName[2]);
                        row.add(String.valueOf(cl));
                        row.add(String.valueOf(damage));
                        row.add(kabau?"1" :"0");
                        if(eflag == 0){
                            if(at < 7){row.addAll(this.ShipRowBodyUpdate(friendRows.get(at-1), prevHP[1][at-1],this.maxFriendHp[at-1]));}
                            else{row.addAll(this.ShipRowBodyUpdate(friendCombinedRows.get(at-7), prevHP[2][at-7],this.maxFriendHpCombined[at-7]));}
                            if(df < 7){row.addAll(this.ShipRowBodyUpdate(enemyRows.get(df-1), prevHP[0][df-1],this.maxEnemyHp[df-1]));}
                            else{row.addAll(this.ShipRowBodyUpdate(enemyCombinedRows.get(df-7), prevHP[3][df-7],this.maxEnemyHpCombined[df-7]));}
                        }else{
                            if(at < 7){row.addAll(this.ShipRowBodyUpdate(enemyRows.get(at-1), prevHP[0][at-1],this.maxEnemyHp[at-1]));}
                            else{row.addAll(this.ShipRowBodyUpdate(enemyCombinedRows.get(at-7), prevHP[3][at-7],this.maxEnemyHpCombined[at-7]));}
                            if(df < 7){row.addAll(this.ShipRowBodyUpdate(friendRows.get(df-1), prevHP[1][df-1],this.maxFriendHp[df-1]));}
                            else{row.addAll(this.ShipRowBodyUpdate(friendCombinedRows.get(df-7), prevHP[2][df-7],this.maxFriendHpCombined[df-7]));}
                        }
                        row.add(combinedFlagString);
                        row.add(isEnemyCombined()?"連合艦隊":"通常艦隊");
                        if(filter.filterHougekiAttackDefenceEC(this, at, df, eflag) && filter.filterOutput(row)){
                            body.add(row);
                        }
                        if(i-1<hougekiHP.size() && j<hougekiHP.get(i-1).size()){
                            prevHP = hougekiHP.get(i-1).get(j);
                        }
                    }
                }
            }
            return prevHP;
        }

    private ArrayList<ArrayList<String>> HougekiRowBody(Phase phase,LinkedTreeMap tree,ArrayList<ArrayList<String>> enemyRows,ArrayList<ArrayList<String>> friendRows,ArrayList<ArrayList<String>> enemyCombinedRows,ArrayList<ArrayList<String>> combinedRows,BuiltinScriptFilter filter){
        ArrayList<ArrayList<String>> body = new ArrayList<ArrayList<String>>();
        if(phase.isNight){
            return body;
        }
        if(enemyRows == null){
            enemyRows = new ArrayList<ArrayList<String>>();
            for(int i=0;i<this.enemy.size();i++){
                enemyRows.add(this.ShipRowBodyBase(this.enemy.get(i), this.maxEnemyHp[i], i));
            }
        }
        if(friendRows == null){
            friendRows = new ArrayList<ArrayList<String>>();
            if(this.getDock()!=null){
                List<ShipDto> ships = this.getDock().getShips();
                for(int i=0;i<ships.size();i++){
                    friendRows.add(this.ShipRowBodyBase(ships.get(i), this.maxFriendHp[i], i));
                }
            }
        }
        if(combinedRows == null && this.isCombined()){
            combinedRows = (this.isCombined()) ?new ArrayList<ArrayList<String>>() :null;
            if(this.isCombined() && this.getDockCombined()!=null){
                List<ShipDto> ships = this.getDockCombined().getShips();
                for(int i=0;i<ships.size();i++){
                    combinedRows.add(this.ShipRowBodyBase(ships.get(i), this.maxFriendHpCombined[i], i+6));
                }
            }
        }
        if(enemyCombinedRows == null && this.isEnemyCombined()){
            enemyCombinedRows = (this.isEnemyCombined()) ?new ArrayList<ArrayList<String>>() :null;
            if(this.isEnemyCombined()){
                for(int i=0;i<this.enemyCombined.size();i++){
                    enemyCombinedRows.add(this.ShipRowBodyBase(this.enemyCombined.get(i), this.maxEnemyHpCombined[i], i+6));
                }
            }
        }
        int combinedFlag = this.getCombinedKind();
        String combinedFlagString =
            (combinedFlag == 0)?"通常艦隊":
            (combinedFlag == 1)?"機動部隊":
            (combinedFlag == 2)?"水上部隊":
            (combinedFlag == 3)?"輸送部隊":
            "不明";

        int[][]phaseStartHP = new int[4][];
        if(phase == this.getPhase1()){
            //敵、味方、味方、敵、順番がひどい
            phaseStartHP[0] = this.startEnemyHp.clone();
            phaseStartHP[1] = this.startFriendHp.clone();
            phaseStartHP[2] = (this.isCombined()) ?this.startFriendHpCombined.clone() :null;
            phaseStartHP[3] = (this.isEnemyCombined()) ? this.startEnemyHpCombined.clone() : null;
        }
        else{
            Phase phase1 = this.getPhase1();
            phaseStartHP[0] = phase1.nowEnemyHp.clone();
            phaseStartHP[1] = phase1.nowFriendHp.clone();
            phaseStartHP[2] = (this.isCombined()) ?phase1.nowFriendHpCombined.clone() :null;
            phaseStartHP[3] = (this.isEnemyCombined()) ?phase1.nowEnemyHpCombined.clone() : null;
        }
        int[][]injectionAirBaseStartHP = phaseStartHP;
        int[][]injectionAirStartHP = this.createNextHPAir(injectionAirBaseStartHP, phase.airBaseInjection);
        int[][]airBaseStartHP = this.createNextHPAir(injectionAirStartHP, phase.airInjection);
        int[][]air1StartHP = airBaseStartHP;
        if(phase.airBase != null){
            ArrayList<int[][]>airHP = this.createNextHPAirBase(airBaseStartHP,phase.airBase);
            air1StartHP = airHP.get(airHP.size()-1);
        }
        int[][]supportStartHP = this.createNextHPAir(air1StartHP, phase.air);
        int[][]openingTaisenStartHP = this.createNextHP(supportStartHP,phase.support);
        int[][]openingRaigekiStartHP = openingTaisenStartHP;
        if(phase.openingTaisen != null){
            LinkedTreeMap api_hougeki = (LinkedTreeMap)tree.get("api_opening_taisen");
            openingRaigekiStartHP =
                this.HougekiRowBodyConstruct(
                    enemyRows,
                    (this.isCombined()?combinedRows :friendRows),
                    phase.openingTaisen,
                    api_hougeki,
                    this.isCombined(),
                    "先制対潜",
                    combinedFlagString,
                    openingTaisenStartHP,
                    body,
                    filter
                );
        }
        int[][]air2StartHP = this.createNextHP(openingRaigekiStartHP, phase.opening);
        int[][]hougeki1StartHP = this.createNextHPAir(air2StartHP, phase.air2);

        int[][]hougeki2StartHP;
        int[][]hougeki3StartHP;
        int[][]raigekiStartHP;
        int[][]endHP;
        if(phase.getKind().isHougeki1Second()){
            raigekiStartHP = hougeki1StartHP;
            if(phase.hougeki1 != null){
                LinkedTreeMap api_hougeki = (LinkedTreeMap)tree.get("api_hougeki1");
                raigekiStartHP =
                    this.HougekiRowBodyConstruct(
                        enemyRows,
                        combinedRows,
                        phase.hougeki1,
                        api_hougeki,
                        true,
                        "1",
                        combinedFlagString,
                        hougeki1StartHP,
                        body,
                        filter
                    );
            }
            hougeki2StartHP = this.createNextHP(raigekiStartHP, phase.raigeki);
            hougeki3StartHP = hougeki2StartHP;
            if(phase.hougeki2 != null){
                LinkedTreeMap api_hougeki = (LinkedTreeMap)tree.get("api_hougeki2");
                hougeki3StartHP =
                    this.HougekiRowBodyConstruct(
                        enemyRows,
                        friendRows,
                        phase.hougeki2,
                        api_hougeki,
                        false,
                        "1",
                        combinedFlagString,
                        hougeki2StartHP,
                        body,
                        filter
                    );
            }
            endHP = hougeki3StartHP;
            if(phase.hougeki3 != null){
                LinkedTreeMap api_hougeki = (LinkedTreeMap)tree.get("api_hougeki3");
                endHP =
                    this.HougekiRowBodyConstruct(
                        enemyRows,
                        friendRows,
                        phase.hougeki3,
                        api_hougeki,
                        false,
                        "2",
                        combinedFlagString,
                        hougeki3StartHP,
                        body,
                        filter
                    );
            }
        }
        else if(this.isCombined() && this.isEnemyCombined() && phase.getKind().isHougeki2Second()){
            hougeki2StartHP = hougeki1StartHP;
            if(phase.hougeki1 != null){
                LinkedTreeMap api_hougeki = (LinkedTreeMap)tree.get("api_hougeki1");
                hougeki2StartHP =
                    this.HougekiRowBodyConstructEC(
                        enemyRows,
                        friendRows,
                        enemyCombinedRows,
                        combinedRows,
                        phase.hougeki1,
                        api_hougeki,
                        "1",
                        combinedFlagString,
                        hougeki1StartHP,
                        body,
                        filter
                    );
            }
            raigekiStartHP = hougeki2StartHP;
            if(phase.hougeki2 != null){
                LinkedTreeMap api_hougeki = (LinkedTreeMap)tree.get("api_hougeki2");
                raigekiStartHP =
                    this.HougekiRowBodyConstructEC(
                        enemyRows,
                        friendRows,
                        enemyCombinedRows,
                        combinedRows,
                        phase.hougeki2,
                        api_hougeki,
                        "2",
                        combinedFlagString,
                        hougeki2StartHP,
                        body,
                        filter
                    );
            }
            hougeki3StartHP = this.createNextHP(raigekiStartHP, phase.raigeki);
            endHP = hougeki3StartHP;
            if(phase.hougeki3 != null){
                LinkedTreeMap api_hougeki = (LinkedTreeMap)tree.get("api_hougeki3");
                endHP =
                    this.HougekiRowBodyConstructEC(
                        enemyRows,
                        friendRows,
                        enemyCombinedRows,
                        combinedRows,
                        phase.hougeki3,
                        api_hougeki,
                        "3",
                        combinedFlagString,
                        hougeki3StartHP,
                        body,
                        filter
                    );
            }
        }
        else if(this.isCombined() && this.isEnemyCombined() && phase.getKind().isHougeki3Second()){
            hougeki2StartHP = hougeki1StartHP;
            if(phase.hougeki1 != null){
                LinkedTreeMap api_hougeki = (LinkedTreeMap)tree.get("api_hougeki1");
                hougeki2StartHP =
                    this.HougekiRowBodyConstructEC(
                        enemyRows,
                        friendRows,
                        enemyCombinedRows,
                        combinedRows,
                        phase.hougeki1,
                        api_hougeki,
                        "1",
                        combinedFlagString,
                        hougeki1StartHP,
                        body,
                        filter
                    );
            }
            hougeki3StartHP = hougeki2StartHP;
            if(phase.hougeki2 != null){
                LinkedTreeMap api_hougeki = (LinkedTreeMap)tree.get("api_hougeki2");
                hougeki3StartHP =
                    this.HougekiRowBodyConstructEC(
                        enemyRows,
                        friendRows,
                        enemyCombinedRows,
                        combinedRows,
                        phase.hougeki2,
                        api_hougeki,
                        "2",
                        combinedFlagString,
                        hougeki2StartHP,
                        body,
                        filter
                    );
            }
            raigekiStartHP = hougeki3StartHP;
            if(phase.hougeki3 != null){
                LinkedTreeMap api_hougeki = (LinkedTreeMap)tree.get("api_hougeki3");
                raigekiStartHP =
                    this.HougekiRowBodyConstructEC(
                        enemyRows,
                        friendRows,
                        enemyCombinedRows,
                        combinedRows,
                        phase.hougeki3,
                        api_hougeki,
                        "3",
                        combinedFlagString,
                        hougeki3StartHP,
                        body,
                        filter
                    );
            }
            endHP = this.createNextHP(raigekiStartHP, phase.raigeki);
        }
        else if(this.isEnemyCombined()){
            raigekiStartHP = hougeki1StartHP;
            if(phase.hougeki1 != null){
                LinkedTreeMap api_hougeki = (LinkedTreeMap)tree.get("api_hougeki1");
                raigekiStartHP =
                    //引数の順番の一貫性の無さがやばいので後で直す
                    this.HougekiRowBodyConstructEC(
                        enemyRows,
                        friendRows,
                        enemyCombinedRows,
                        combinedRows,
                        phase.hougeki1,
                        api_hougeki,
                        "1",
                        combinedFlagString,
                        hougeki1StartHP,
                        body,
                        filter
                    );
            }
            hougeki2StartHP = this.createNextHP(raigekiStartHP, phase.raigeki);
            hougeki3StartHP = hougeki2StartHP;
            if(phase.hougeki2 != null){
                LinkedTreeMap api_hougeki = (LinkedTreeMap)tree.get("api_hougeki2");
                hougeki3StartHP =
                    this.HougekiRowBodyConstructEC(
                        enemyRows,
                        friendRows,
                        enemyCombinedRows,
                        combinedRows,
                        phase.hougeki2,
                        api_hougeki,
                        "2",
                        combinedFlagString,
                        hougeki2StartHP,
                        body,
                        filter
                    );
            }
            endHP = hougeki3StartHP;
            if(phase.hougeki3 != null){
                LinkedTreeMap api_hougeki = (LinkedTreeMap)tree.get("api_hougeki3");
                endHP =
                    this.HougekiRowBodyConstructEC(
                        enemyRows,
                        friendRows,
                        enemyCombinedRows,
                        combinedRows,
                        phase.hougeki3,
                        api_hougeki,
                        "3",
                        combinedFlagString,
                        hougeki3StartHP,
                        body,
                        filter
                    );
            }
        }
        else{
            hougeki2StartHP = hougeki1StartHP;
            if(phase.hougeki1 != null){
                LinkedTreeMap api_hougeki = (LinkedTreeMap)tree.get("api_hougeki1");
                hougeki2StartHP =
                    this.HougekiRowBodyConstruct(
                        enemyRows,
                        friendRows,
                        phase.hougeki1,
                        api_hougeki,
                        false,
                        "1",
                        combinedFlagString,
                        hougeki1StartHP,
                        body,
                        filter
                    );
            }
            hougeki3StartHP = hougeki2StartHP;
            if(phase.hougeki2 != null){
                LinkedTreeMap api_hougeki = (LinkedTreeMap)tree.get("api_hougeki2");
                hougeki3StartHP =
                    this.HougekiRowBodyConstruct(
                        enemyRows,
                        friendRows,
                        phase.hougeki2,
                        api_hougeki,
                        false,
                        "2",
                        combinedFlagString,
                        hougeki2StartHP,
                        body,
                        filter
                    );
            }
            raigekiStartHP = hougeki3StartHP;
            if(phase.hougeki3 != null){
                LinkedTreeMap api_hougeki = (LinkedTreeMap)tree.get("api_hougeki3");
                raigekiStartHP =
                    this.HougekiRowBodyConstruct(
                        enemyRows,
                        combinedRows,
                        phase.hougeki3,
                        api_hougeki,
                        true,
                        "2",
                        combinedFlagString,
                        hougeki3StartHP,
                        body,
                        filter
                    );
            }
            endHP = this.createNextHP(raigekiStartHP, phase.raigeki);
        }
        return body;
    }

    static public ArrayList<String> YasenRowHeader(){
        ArrayList<String> header = NightPhaseRowHeader();
        header.add("戦闘種別");
        header.add("自艦隊");
        header.add("開始");
        header.add("攻撃艦");
        header.add("砲撃種別");
        header.add("表示装備1");
        header.add("表示装備2");
        header.add("表示装備3");
        header.add("クリティカル");
        header.add("ダメージ");
        header.add("かばう");
        ArrayList<String> shipHeader = ShipRowHeader();
        int length = shipHeader.size();
        for(int i=0;i<length;i++){
            header.add(String.format("攻撃艦.%s",shipHeader.get(i)));
        }
        for(int i=0;i<length;i++){
            header.add(String.format("防御艦.%s",shipHeader.get(i)));
        }
        header.add("艦隊種類");
        header.add("敵艦隊種類");
        return header;
    }

    private int[][] YasenRowBodyConstruct(
            ArrayList<ArrayList<String>>enemyRows,
            ArrayList<ArrayList<String>>friendRows,
            List<BattleAtackDto>attackList,
            LinkedTreeMap api_hougeki,
            String spMidnightString,
            String combinedFlagString,
            int[][]startHP,
            ArrayList<ArrayList<String>> body,
            BuiltinScriptFilter filter
            )
        {
            int[][]prevHP = startHP;
            if(attackList != null){
                boolean isSecond = this.isCombined();
                String fleetName =
                    (!this.isCombined())?"通常艦隊"
                    :(isSecond)?"連合第2艦隊"
                    :"連合第1艦隊";
                ArrayList<ArrayList<int[][]>>hougekiHP = this.createNextHPHougeki(startHP, attackList);

                int[] api_at_list = GsonUtil.toIntArray(api_hougeki.get("api_at_list"));
                int[] api_sp_list = GsonUtil.toIntArray(api_hougeki.get("api_sp_list"));
                int[][] api_df_list = GsonUtil.toIntArrayArray(api_hougeki.get("api_df_list"));
                int[][] api_si_list = GsonUtil.toIntArrayArray(api_hougeki.get("api_si_list"));
                int[][] api_cl_list = GsonUtil.toIntArrayArray(api_hougeki.get("api_cl_list"));
                double[][] api_damage = GsonUtil.toDoubleArrayArray(api_hougeki.get("api_damage"));
                List<EnemyShipDto> enemyLisy = this.getEnemy();
                List<ShipDto> friendList = (isSecond)? this.getDockCombined().getShips() :this.getDock().getShips();
                ArrayList<String> nightPhaseRow = this.NightPhaseRowBody();
                for(int i=1;i<api_at_list.length;++i){
                    int at = api_at_list[i];
                    int sp = api_sp_list[i];
                    int[] dfList = api_df_list[i];
                    int[] siList = api_si_list[i];
                    int[] clList = api_cl_list[i];
                    double[] damageList = api_damage[i];

                    String attackFleetName = (at<7)?"自軍" :"敵軍";
                    String[] itemName = new String[3];
                    for(int j=0;j<itemName.length;j++){
                        if(j < siList.length && siList[j] > 0){
                            itemName[j]=Item.get(siList[j]).getName();
                        }else{
                            itemName[j]="";
                        }
                    }

                    List<ItemInfoDto> itemInfoList = (at<7)? friendList.get(at-1).getItem() :enemyLisy.get(at-7).getItem();
                    for(int j=0;j<dfList.length;++j){
                        int cl = clList[j];
                        if(cl >= 0){
                            int df = dfList[j];
                            int damage = (int)damageList[j];
                            boolean kabau = damageList[j] - (double)damage > 0.05;
                            ArrayList<String> row = (ArrayList<String>) nightPhaseRow.clone();
                            row.add("夜戦");
                            row.add(fleetName);
                            row.add(spMidnightString);
                            row.add(attackFleetName);
                            row.add(String.valueOf(sp));
                            row.add(itemName[0]);
                            row.add(itemName[1]);
                            row.add(itemName[2]);
                            row.add(String.valueOf(cl));
                            row.add(String.valueOf(damage));
                            row.add(kabau?"1" :"0");
                            if(at < 7){row.addAll(this.ShipRowBodyUpdate(friendRows.get(at-1), prevHP[isSecond?2 :1][at-1],isSecond?this.maxFriendHpCombined[at-1] :this.maxFriendHp[at-1]));}
                            else{row.addAll(this.ShipRowBodyUpdate(enemyRows.get(at-7), prevHP[0][at-7],this.maxEnemyHp[at-7]));}
                            if(df < 7){row.addAll(this.ShipRowBodyUpdate(friendRows.get(df-1), prevHP[isSecond?2 :1][df-1],isSecond?this.maxFriendHpCombined[df-1] :this.maxFriendHp[df-1]));}
                            else{row.addAll(this.ShipRowBodyUpdate(enemyRows.get(df-7), prevHP[0][df-7],this.maxEnemyHp[df-7]));}
                            row.add(combinedFlagString);
                            row.add(isEnemyCombined()?"連合艦隊":"通常艦隊");
                            if(filter.filterHougekiAttackDefence(this, at, df, isSecond) && filter.filterOutput(row)){
                                body.add(row);
                            }
                            if(i-1<hougekiHP.size() && j<hougekiHP.get(i-1).size()){
                                prevHP = hougekiHP.get(i-1).get(j);
                            }
                        }
                    }
                }
            }
            return prevHP;
        }

    private int[][] YasenRowBodyConstructEC(
            ArrayList<ArrayList<String>>enemyRows,
            ArrayList<ArrayList<String>>friendRows,
            ArrayList<ArrayList<String>>enemyCombinedRows,
            ArrayList<ArrayList<String>>friendCombinedRows,
            int[]activeDeck,
            List<BattleAtackDto>attackList,
            LinkedTreeMap api_hougeki,
            String spMidnightString,
            String combinedFlagString,
            int[][]startHP,
            ArrayList<ArrayList<String>> body,
            BuiltinScriptFilter filter
            )
        {
            int[][]prevHP = startHP;
            if(attackList != null){
                boolean isSecond = this.isCombined();
                boolean enemyIsSecond = (activeDeck[1]==2);
                String fleetName =
                    (!this.isCombined())?"通常艦隊"
                    :(isSecond)?"連合第2艦隊"
                    :"連合第1艦隊";
                ArrayList<ArrayList<int[][]>>hougekiHP = this.createNextHPHougeki(startHP, attackList);

                int[] api_at_list = GsonUtil.toIntArray(api_hougeki.get("api_at_list"));
                int[] api_sp_list = GsonUtil.toIntArray(api_hougeki.get("api_sp_list"));
                int[][] api_df_list = GsonUtil.toIntArrayArray(api_hougeki.get("api_df_list"));
                int[][] api_si_list = GsonUtil.toIntArrayArray(api_hougeki.get("api_si_list"));
                int[][] api_cl_list = GsonUtil.toIntArrayArray(api_hougeki.get("api_cl_list"));
                double[][] api_damage = GsonUtil.toDoubleArrayArray(api_hougeki.get("api_damage"));
                ArrayList<String> nightPhaseRow = this.NightPhaseRowBody();
                for(int i=1;i<api_at_list.length;++i){
                    int at = api_at_list[i];
                    int sp = api_sp_list[i];
                    int[] dfList = api_df_list[i];
                    int[] siList = api_si_list[i];
                    int[] clList = api_cl_list[i];
                    double[] damageList = api_damage[i];

                    String attackFleetName = (at<7)?"自軍" :"敵軍";
                    String[] itemName = new String[3];
                    for(int j=0;j<itemName.length;j++){
                        if(j < siList.length && siList[j] > 0){
                            itemName[j]=Item.get(siList[j]).getName();
                        }else{
                            itemName[j]="";
                        }
                    }
                    for(int j=0;j<dfList.length;++j){
                        int cl = clList[j];
                        if(cl >= 0){
                            int df = dfList[j];
                            int damage = (int)damageList[j];
                            boolean kabau = damageList[j] - (double)damage > 0.05;
                            ArrayList<String> row = (ArrayList<String>) nightPhaseRow.clone();
                            row.add("夜戦");
                            row.add(fleetName);
                            row.add(spMidnightString);
                            row.add(attackFleetName);
                            row.add(String.valueOf(sp));
                            row.add(itemName[0]);
                            row.add(itemName[1]);
                            row.add(itemName[2]);
                            row.add(String.valueOf(cl));
                            row.add(String.valueOf(damage));
                            row.add(kabau?"1" :"0");
                            if(at < 7){row.addAll(this.ShipRowBodyUpdate(isSecond?friendCombinedRows.get(at-1):friendRows.get(at-1), prevHP[isSecond?2 :1][at-1],isSecond?this.maxFriendHpCombined[at-1] :this.maxFriendHp[at-1]));}
                            else{row.addAll(this.ShipRowBodyUpdate(enemyIsSecond?enemyCombinedRows.get(at-7):enemyRows.get(at-7), prevHP[enemyIsSecond?3:0][at-7],enemyIsSecond?this.maxEnemyHpCombined[at-7]:this.maxEnemyHp[at-7]));}
                            if(df < 7){row.addAll(this.ShipRowBodyUpdate(isSecond?friendCombinedRows.get(df-1):friendRows.get(df-1), prevHP[isSecond?2 :1][df-1],isSecond?this.maxFriendHpCombined[df-1] :this.maxFriendHp[df-1]));}
                            else{row.addAll(this.ShipRowBodyUpdate(enemyIsSecond?enemyCombinedRows.get(df-7):enemyRows.get(df-7), prevHP[enemyIsSecond?3:0][df-7],enemyIsSecond?this.maxEnemyHpCombined[df-7]:this.maxEnemyHp[df-7]));}
                            row.add(combinedFlagString);
                            row.add(isEnemyCombined()?"連合艦隊":"通常艦隊");
                            if(filter.filterHougekiAttackDefenceECNight(this, at, df, isSecond,enemyIsSecond) && filter.filterOutput(row)){
                                body.add(row);
                            }
                            if(i-1<hougekiHP.size() && j<hougekiHP.get(i-1).size()){
                                prevHP = hougekiHP.get(i-1).get(j);
                            }
                        }
                    }
                }
            }
            return prevHP;
        }

    private ArrayList<ArrayList<String>> YasenRowBody(Phase phase, LinkedTreeMap tree,ArrayList<ArrayList<String>> enemyRows,ArrayList<ArrayList<String>> friendRows,ArrayList<ArrayList<String>> enemyCombinedRows,ArrayList<ArrayList<String>> combinedRows,BuiltinScriptFilter filter){
        ArrayList<ArrayList<String>> body = new ArrayList<ArrayList<String>>();
        if(!phase.isNight){
            return body;
        }
        if(enemyRows == null){
            enemyRows = new ArrayList<ArrayList<String>>();
            for(int i=0;i<this.enemy.size();i++){
                enemyRows.add(this.ShipRowBodyBase(this.enemy.get(i), this.maxEnemyHp[i], i));
            }
        }
        if(friendRows == null){
            friendRows = new ArrayList<ArrayList<String>>();
            if(this.getDock()!=null){
                List<ShipDto> ships = this.getDock().getShips();
                for(int i=0;i<ships.size();i++){
                    friendRows.add(this.ShipRowBodyBase(ships.get(i), this.maxFriendHp[i], i));
                }
            }
        }
        if(combinedRows == null && this.isCombined()){
            combinedRows = (this.isCombined()) ?new ArrayList<ArrayList<String>>() :null;
            if(this.isCombined() && this.getDockCombined()!=null){
                List<ShipDto> ships = this.getDockCombined().getShips();
                for(int i=0;i<ships.size();i++){
                    combinedRows.add(this.ShipRowBodyBase(ships.get(i), this.maxFriendHpCombined[i], i+6));
                }
            }
        }
        if(enemyCombinedRows == null && this.isEnemyCombined()){
            enemyCombinedRows = (this.isEnemyCombined()) ?new ArrayList<ArrayList<String>>() :null;
            if(this.isEnemyCombined()){
                for(int i=0;i<this.enemyCombined.size();i++){
                    enemyCombinedRows.add(this.ShipRowBodyBase(this.enemyCombined.get(i), this.maxEnemyHpCombined[i], i+6));
                }
            }
        }
        int combinedFlag = this.getCombinedKind();
        String combinedFlagString =
            (combinedFlag == 0)?"通常艦隊":
            (combinedFlag == 1)?"機動部隊":
            (combinedFlag == 2)?"水上部隊":
            (combinedFlag == 3)?"輸送部隊":
            "不明";

        int[][]phaseStartHP = new int[4][];
        if(phase == this.getPhase1()){
            //敵、味方、味方、敵、順番がひどい
            phaseStartHP[0] = this.startEnemyHp.clone();
            phaseStartHP[1] = this.startFriendHp.clone();
            phaseStartHP[2] = (this.isCombined()) ?this.startFriendHpCombined.clone() :null;
            phaseStartHP[3] = (this.isEnemyCombined()) ? this.startEnemyHpCombined.clone() : null;
        }
        else{
            Phase phase1 = this.getPhase1();
            phaseStartHP[0] = phase1.nowEnemyHp.clone();
            phaseStartHP[1] = phase1.nowFriendHp.clone();
            phaseStartHP[2] = (this.isCombined()) ?phase1.nowFriendHpCombined.clone() :null;
            phaseStartHP[3] = (this.isEnemyCombined()) ?phase1.nowEnemyHpCombined.clone() : null;
        }
        int[][]endHP = phaseStartHP;
        if(phase.hougeki != null){
            LinkedTreeMap api_hougeki = (LinkedTreeMap)tree.get("api_hougeki");
            endHP =
                (tree.containsKey("api_active_deck"))
                    ?this.YasenRowBodyConstructEC(
                            enemyRows,
                            friendRows,
                            enemyCombinedRows,
                            combinedRows,
                            GsonUtil.toIntArray(tree.get("api_active_deck")),
                            phase.hougeki,
                            api_hougeki,
                            (phase == this.getPhase1())?"夜戦開始" :"昼戦開始",
                            combinedFlagString,
                            phaseStartHP,
                            body,
                            filter
                        )
                    :this.YasenRowBodyConstruct(
                        enemyRows,
                        (this.isCombined())?combinedRows :friendRows,
                        phase.hougeki,
                        api_hougeki,
                        (phase == this.getPhase1())?"夜戦開始" :"昼戦開始",
                        combinedFlagString,
                        phaseStartHP,
                        body,
                        filter
                    );
        }
        return body;
    }

    static public ArrayList<String> RaigekiRowHeader(){
        ArrayList<String> header = DayPhaseRowHeader();
        header.add("戦闘種別");
        header.add("自艦隊");
        header.add("開幕|閉幕");
        header.add("攻撃艦");
        header.add("種別");
        header.add("表示装備1");
        header.add("表示装備2");
        header.add("表示装備3");
        header.add("クリティカル");
        header.add("ダメージ");
        header.add("かばう");
        ArrayList<String> shipHeader = ShipRowHeader();
        int length = shipHeader.size();
        for(int i=0;i<length;i++){
            header.add(String.format("攻撃艦.%s",shipHeader.get(i)));
        }
        for(int i=0;i<length;i++){
            header.add(String.format("防御艦.%s",shipHeader.get(i)));
        }
        header.add("艦隊種類");
        header.add("敵艦隊種類");
        return header;
    }

    private int[][] RaigekiRowBodyConstruct(
            ArrayList<ArrayList<String>>enemyRows,
            ArrayList<ArrayList<String>>friendRows,
            List<BattleAtackDto>attackList,
            LinkedTreeMap api_raigeki,
            String stage,
            String combinedFlagString,
            int[][]startHP,
            ArrayList<ArrayList<String>> body,
            BuiltinScriptFilter filter)
        {
            int[][]prevHP = startHP;
            boolean isSecond = this.isCombined();
            String fleetName =
                (!this.isCombined())?"通常艦隊"
                :(isSecond)?"連合第2艦隊"
                :"連合第1艦隊";

            int[] api_frai = GsonUtil.toIntArray(api_raigeki.get("api_frai"));
            int[] api_erai = GsonUtil.toIntArray(api_raigeki.get("api_erai"));
            double[] api_fdam = GsonUtil.toDoubleArray(api_raigeki.get("api_fdam"));
            double[] api_edam = GsonUtil.toDoubleArray(api_raigeki.get("api_edam"));
            int[] api_fydam = GsonUtil.toIntArray(api_raigeki.get("api_fydam"));
            int[] api_eydam = GsonUtil.toIntArray(api_raigeki.get("api_eydam"));
            int[] api_fcl = GsonUtil.toIntArray(api_raigeki.get("api_fcl"));
            int[] api_ecl = GsonUtil.toIntArray(api_raigeki.get("api_ecl"));

            List<EnemyShipDto> enemyLisy = this.getEnemy();
            List<ShipDto> friendList = (isSecond)? this.getDockCombined().getShips() :this.getDock().getShips();
            ArrayList<String> dayPhaseRow = this.DayPhaseRowBody();

            for(int i=1;i<=6;++i){
                int at = i;
                int df = api_frai[i];
                if(df <= 0){ continue; }
                int cl = api_fcl[i];
                int ydam = api_fydam[i];
                boolean kabau = ((int)(api_edam[df]*100))%100 > 5;
                ArrayList<String> row = (ArrayList<String>)dayPhaseRow.clone();
                row.add("雷撃戦");
                row.add(fleetName);
                row.add(stage);
                row.add("自軍");
                row.add("");
                row.add("");
                row.add("");
                row.add("");
                row.add(String.valueOf(cl));
                row.add(String.valueOf(ydam));
                row.add((kabau)?"1" :"0");
                row.addAll(this.ShipRowBodyUpdate(friendRows.get(at-1), prevHP[isSecond?2 :1][at-1],isSecond?this.maxFriendHpCombined[at-1] :this.maxFriendHp[at-1]));
                row.addAll(this.ShipRowBodyUpdate(enemyRows.get(df-1), prevHP[0][df-1],this.maxEnemyHp[df-1]));
                row.add(combinedFlagString);
                row.add(isEnemyCombined()?"連合艦隊":"通常艦隊");
                if(filter.filterRaigekiAttackDefence(this, at, df, isSecond, true) && filter.filterOutput(row)){
                    body.add(row);
                }
            }
            for(int i=1;i<=6;++i){
                int at = i;
                int df = api_erai[i];
                if(df <= 0){ continue; }
                int cl = api_ecl[i];
                int ydam = api_eydam[i];
                boolean kabau = ((int)(api_fdam[df]*100))%100 > 5;
                ArrayList<String> row = (ArrayList<String>)dayPhaseRow.clone();
                row.add("雷撃戦");
                row.add(fleetName);
                row.add(stage);
                row.add("敵軍");
                row.add("");
                row.add("");
                row.add("");
                row.add("");
                row.add(String.valueOf(cl));
                row.add(String.valueOf(ydam));
                row.add((kabau)?"1" :"0");
                row.addAll(this.ShipRowBodyUpdate(enemyRows.get(at-1), prevHP[0][at-1],this.maxEnemyHp[at-1]));
                row.addAll(this.ShipRowBodyUpdate(friendRows.get(df-1), prevHP[isSecond?2 :1][df-1],isSecond?this.maxFriendHpCombined[df-1] :this.maxFriendHp[df-1]));
                row.add(combinedFlagString);
                row.add(isEnemyCombined()?"連合艦隊":"通常艦隊");
                if(filter.filterRaigekiAttackDefence(this, at, df, isSecond, false) && filter.filterOutput(row)){
                    body.add(row);
                }
            }
            return createNextHP(prevHP, attackList);
        }

    private int[][] RaigekiRowBodyConstructEC(
            ArrayList<ArrayList<String>>enemyRows,
            ArrayList<ArrayList<String>>friendRows,
            ArrayList<ArrayList<String>>enemyCombinedRows,
            ArrayList<ArrayList<String>>friendCombinedRows,
            List<BattleAtackDto>attackList,
            LinkedTreeMap api_raigeki,
            String stage,
            String combinedFlagString,
            int[][]startHP,
            ArrayList<ArrayList<String>> body,
            BuiltinScriptFilter filter)
        {
            int[][]prevHP = startHP;

            int[] api_frai = GsonUtil.toIntArray(api_raigeki.get("api_frai"));
            int[] api_erai = GsonUtil.toIntArray(api_raigeki.get("api_erai"));
            double[] api_fdam = GsonUtil.toDoubleArray(api_raigeki.get("api_fdam"));
            double[] api_edam = GsonUtil.toDoubleArray(api_raigeki.get("api_edam"));
            int[] api_fydam = GsonUtil.toIntArray(api_raigeki.get("api_fydam"));
            int[] api_eydam = GsonUtil.toIntArray(api_raigeki.get("api_eydam"));
            int[] api_fcl = GsonUtil.toIntArray(api_raigeki.get("api_fcl"));
            int[] api_ecl = GsonUtil.toIntArray(api_raigeki.get("api_ecl"));
            ArrayList<String> dayPhaseRow = this.DayPhaseRowBody();

            for(int i=1;i<=12;++i){
                int at = i;
                int df = api_frai[i];
                boolean isSecond = at >= 7;
                boolean enemyIsSecond = df >= 7;
                String fleetName =
                    (!this.isCombined())?"通常艦隊"
                    :(isSecond)?"連合第2艦隊"
                    :"連合第1艦隊";
                if(df <= 0){ continue; }
                int cl = api_fcl[i];
                int ydam = api_fydam[i];
                boolean kabau = ((int)(api_edam[df]*100))%100 > 5;
                ArrayList<String> row = (ArrayList<String>)dayPhaseRow.clone();
                row.add("雷撃戦");
                row.add(fleetName);
                row.add(stage);
                row.add("自軍");
                row.add("");
                row.add("");
                row.add("");
                row.add("");
                row.add(String.valueOf(cl));
                row.add(String.valueOf(ydam));
                row.add((kabau)?"1" :"0");
                if(isSecond){ row.addAll(this.ShipRowBodyUpdate(friendCombinedRows.get(at-7), prevHP[2][at-7],this.maxFriendHpCombined[at-7])); }
                else{ row.addAll(this.ShipRowBodyUpdate(friendRows.get(at-1), prevHP[1][at-1],this.maxFriendHp[at-1])); }
                if(enemyIsSecond){row.addAll(this.ShipRowBodyUpdate(enemyCombinedRows.get(df-7), prevHP[3][df-7],this.maxEnemyHpCombined[df-7]));}
                else{row.addAll(this.ShipRowBodyUpdate(enemyRows.get(df-1), prevHP[0][df-1],this.maxEnemyHp[df-1]));}
                row.add(combinedFlagString);
                row.add(isEnemyCombined()?"連合艦隊":"通常艦隊");
                if(filter.filterRaigekiAttackDefenceEC(this, at, df, true) && filter.filterOutput(row)){
                    body.add(row);
                }
            }
            for(int i=1;i<=12;++i){
                int at = i;
                int df = api_erai[i];
                boolean isSecond = df >= 7;
                boolean enemyIsSecond = at >= 7;
                String fleetName =
                    (!this.isCombined())?"通常艦隊"
                    :(isSecond)?"連合第2艦隊"
                    :"連合第1艦隊";
                if(df <= 0){ continue; }
                int cl = api_ecl[i];
                int ydam = api_eydam[i];
                boolean kabau = ((int)(api_fdam[df]*100))%100 > 5;
                ArrayList<String> row = (ArrayList<String>)dayPhaseRow.clone();
                row.add("雷撃戦");
                row.add(fleetName);
                row.add(stage);
                row.add("敵軍");
                row.add("");
                row.add("");
                row.add("");
                row.add("");
                row.add(String.valueOf(cl));
                row.add(String.valueOf(ydam));
                row.add((kabau)?"1" :"0");
                if(enemyIsSecond){row.addAll(this.ShipRowBodyUpdate(enemyCombinedRows.get(at-7), prevHP[3][at-7],this.maxEnemyHpCombined[at-7]));}
                else{row.addAll(this.ShipRowBodyUpdate(enemyRows.get(at-1), prevHP[0][at-1],this.maxEnemyHp[at-1]));}
                if(isSecond){ row.addAll(this.ShipRowBodyUpdate(friendCombinedRows.get(df-7), prevHP[2][df-7],this.maxFriendHpCombined[df-7])); }
                else{ row.addAll(this.ShipRowBodyUpdate(friendRows.get(df-1), prevHP[1][df-1],this.maxFriendHp[df-1])); }
                row.add(combinedFlagString);
                row.add(isEnemyCombined()?"連合艦隊":"通常艦隊");
                if(filter.filterRaigekiAttackDefenceEC(this, at, df, false) && filter.filterOutput(row)){
                    body.add(row);
                }
            }
            return createNextHP(prevHP, attackList);
        }


    private ArrayList<ArrayList<String>>RaigekiRowBody(Phase phase,LinkedTreeMap tree,ArrayList<ArrayList<String>> enemyRows,ArrayList<ArrayList<String>> friendRows,ArrayList<ArrayList<String>> enemyCombinedRows,ArrayList<ArrayList<String>> combinedRows,BuiltinScriptFilter filter){
        ArrayList<ArrayList<String>> body = new ArrayList<ArrayList<String>>();
        if(phase.isNight){
            return body;
        }
        if(enemyRows == null){
            enemyRows = new ArrayList<ArrayList<String>>();
            for(int i=0;i<this.enemy.size();i++){
                enemyRows.add(this.ShipRowBodyBase(this.enemy.get(i), this.maxEnemyHp[i], i));
            }
        }
        if(friendRows == null){
            friendRows = new ArrayList<ArrayList<String>>();
            if(this.getDock()!=null){
                List<ShipDto> ships = this.getDock().getShips();
                for(int i=0;i<ships.size();i++){
                    friendRows.add(this.ShipRowBodyBase(ships.get(i), this.maxFriendHp[i], i));
                }
            }
        }
        if(combinedRows == null && this.isCombined()){
            combinedRows = (this.isCombined()) ?new ArrayList<ArrayList<String>>() :null;
            if(this.isCombined() && this.getDockCombined()!=null){
                List<ShipDto> ships = this.getDockCombined().getShips();
                for(int i=0;i<ships.size();i++){
                    combinedRows.add(this.ShipRowBodyBase(ships.get(i), this.maxFriendHpCombined[i], i+6));
                }
            }
        }
        if(enemyCombinedRows == null && this.isEnemyCombined()){
            enemyCombinedRows = (this.isEnemyCombined()) ?new ArrayList<ArrayList<String>>() :null;
            if(this.isEnemyCombined()){
                for(int i=0;i<this.enemyCombined.size();i++){
                    enemyCombinedRows.add(this.ShipRowBodyBase(this.enemyCombined.get(i), this.maxEnemyHpCombined[i], i+6));
                }
            }
        }
        int combinedFlag = this.getCombinedKind();
        String combinedFlagString =
            (combinedFlag == 0)?"通常艦隊":
            (combinedFlag == 1)?"機動部隊":
            (combinedFlag == 2)?"水上部隊":
            (combinedFlag == 3)?"輸送部隊":
            "不明";

        int[][]phaseStartHP = new int[4][];
        if(phase == this.getPhase1()){
            phaseStartHP[0] = this.startEnemyHp.clone();
            phaseStartHP[1] = this.startFriendHp.clone();
            phaseStartHP[2] = (this.isCombined()) ?this.startFriendHpCombined.clone() :null;
            phaseStartHP[3] = (this.isEnemyCombined()) ? this.startEnemyHpCombined.clone() : null;
        }
        else{
            Phase phase1 = this.getPhase1();
            phaseStartHP[0] = phase1.nowEnemyHp.clone();
            phaseStartHP[1] = phase1.nowFriendHp.clone();
            phaseStartHP[2] = (this.isCombined()) ?phase1.nowFriendHpCombined.clone() :null;
            phaseStartHP[3] = (this.isEnemyCombined()) ?phase1.nowEnemyHpCombined.clone() : null;
        }
        int[][]injectionAirBaseStartHP = phaseStartHP;
        int[][]injectionAirStartHP = this.createNextHPAir(injectionAirBaseStartHP, phase.airBaseInjection);
        int[][]airBaseStartHP = this.createNextHPAir(injectionAirStartHP, phase.airInjection);
        int[][]air1StartHP = airBaseStartHP;
        if(phase.airBase != null){
            ArrayList<int[][]>airHP = this.createNextHPAirBase(airBaseStartHP,phase.airBase);
            air1StartHP = airHP.get(airHP.size()-1);
        }
        int[][]supportStartHP = this.createNextHPAir(air1StartHP, phase.air);
        int[][]openingTaisenStartHP = this.createNextHP(supportStartHP,phase.support);
        int[][]openingRaigekiStartHP = this.createNextHP(openingTaisenStartHP,phase.openingTaisen);
        int[][]air2StartHP = openingRaigekiStartHP;
        if(phase.opening != null){
            LinkedTreeMap api_raigeki = (LinkedTreeMap)tree.get("api_opening_atack");
            air2StartHP =
                (this.isEnemyCombined())
                    ?this.RaigekiRowBodyConstructEC(
                        enemyRows,
                        friendRows,
                        enemyCombinedRows,
                        combinedRows,
                        phase.opening,
                        api_raigeki,
                        "開幕",
                        combinedFlagString,
                        openingRaigekiStartHP,
                        body,
                        filter
                    )
                    :this.RaigekiRowBodyConstruct(
                        enemyRows,
                        (this.isCombined()?combinedRows :friendRows),
                        phase.opening,
                        api_raigeki,
                        "開幕",
                        combinedFlagString,
                        openingRaigekiStartHP,
                        body,
                        filter
                    );
        }
        int[][]hougeki1StartHP = this.createNextHPAir(air2StartHP, phase.air2);

        int[][]hougeki2StartHP;
        int[][]hougeki3StartHP;
        int[][]raigekiStartHP;
        int[][]endHP;
        if(this.isEnemyCombined()){
            int x = 0;
        }
        if(phase.getKind().isHougeki1Second() || (!this.isCombined() && this.isEnemyCombined())){
            raigekiStartHP = createNextHP(hougeki1StartHP,phase.hougeki1);
            hougeki2StartHP = raigekiStartHP;
            if(phase.raigeki != null){
                LinkedTreeMap api_raigeki = (LinkedTreeMap)tree.get("api_raigeki");
                hougeki2StartHP =
                    (this.isEnemyCombined())
                        ?this.RaigekiRowBodyConstructEC(
                            enemyRows,
                            friendRows,
                            enemyCombinedRows,
                            combinedRows,
                            phase.raigeki,
                            api_raigeki,
                            "閉幕",
                            combinedFlagString,
                            raigekiStartHP,
                            body,
                            filter
                        )
                        :this.RaigekiRowBodyConstruct(
                            enemyRows,
                            (this.isCombined()?combinedRows :friendRows),
                            phase.raigeki,
                            api_raigeki,
                            "閉幕",
                            combinedFlagString,
                            raigekiStartHP,
                            body,
                            filter
                        );
            }
            hougeki3StartHP = createNextHP(hougeki2StartHP, phase.hougeki2);
            endHP = createNextHP(hougeki3StartHP,phase.hougeki3);
        }
        else if(this.isCombined() && this.isEnemyCombined() && phase.getKind().isHougeki2Second()){
            hougeki2StartHP = createNextHP(hougeki1StartHP,phase.hougeki1);
            raigekiStartHP = createNextHP(hougeki2StartHP, phase.hougeki2);
            hougeki3StartHP = raigekiStartHP;
            if(phase.raigeki != null){
                LinkedTreeMap api_raigeki = (LinkedTreeMap)tree.get("api_raigeki");
                hougeki3StartHP =
                    this.RaigekiRowBodyConstructEC(
                        enemyRows,
                        friendRows,
                        enemyCombinedRows,
                        combinedRows,
                        phase.raigeki,
                        api_raigeki,
                        "閉幕",
                        combinedFlagString,
                        raigekiStartHP,
                        body,
                        filter
                    );
            }
            endHP = createNextHP(hougeki3StartHP,phase.hougeki3);
        }
        else if(this.isCombined() && this.isEnemyCombined() && phase.getKind().isHougeki3Second()){
            hougeki2StartHP = createNextHP(hougeki1StartHP,phase.hougeki1);
            hougeki3StartHP = createNextHP(hougeki2StartHP, phase.hougeki2);
            raigekiStartHP = createNextHP(hougeki3StartHP,phase.hougeki3);
            endHP = raigekiStartHP;
            if(phase.raigeki != null){
                LinkedTreeMap api_raigeki = (LinkedTreeMap)tree.get("api_raigeki");
                endHP =
                    this.RaigekiRowBodyConstructEC(
                        enemyRows,
                        friendRows,
                        enemyCombinedRows,
                        combinedRows,
                        phase.raigeki,
                        api_raigeki,
                        "閉幕",
                        combinedFlagString,
                        raigekiStartHP,
                        body,
                        filter
                    );
            }
        }
        else{
            hougeki2StartHP = createNextHP(hougeki1StartHP,phase.hougeki1);
            hougeki3StartHP = createNextHP(hougeki2StartHP,phase.hougeki2);
            raigekiStartHP = createNextHP(hougeki3StartHP,phase.hougeki3);
            endHP = raigekiStartHP;
            if(phase.raigeki != null){
                LinkedTreeMap api_raigeki = (LinkedTreeMap)tree.get("api_raigeki");
                endHP =
                    (this.isEnemyCombined())
                        ?this.RaigekiRowBodyConstructEC(
                            enemyRows,
                            friendRows,
                            enemyCombinedRows,
                            combinedRows,
                            phase.raigeki,
                            api_raigeki,
                            "閉幕",
                            combinedFlagString,
                            raigekiStartHP,
                            body,
                            filter
                        )
                        :this.RaigekiRowBodyConstruct(
                            enemyRows,
                            (this.isCombined()?combinedRows :friendRows),
                            phase.raigeki,
                            api_raigeki,
                            "閉幕",
                            combinedFlagString,
                            raigekiStartHP,
                            body,
                            filter
                        );
            }
        }
        return body;
    }


    static public ArrayList<String> AirRowHeader(){
        ArrayList<String> header = DayPhaseRowHeader();
        header.add("ステージ1.自艦載機総数");
        header.add("ステージ1.自艦載機喪失数");
        header.add("ステージ1.敵艦載機総数");
        header.add("ステージ1.敵艦載機喪失数");
        header.add("ステージ2.自艦載機総数");
        header.add("ステージ2.自艦載機喪失数");
        header.add("ステージ2.敵艦載機総数");
        header.add("ステージ2.敵艦載機喪失数");
        header.add("対空カットイン.発動艦");
        header.add("対空カットイン.種別");
        header.add("対空カットイン.表示装備1");
        header.add("対空カットイン.表示装備2");
        header.add("対空カットイン.表示装備3");
        for(int i=1;i <= 6; ++i){
            String index = (new Integer(i)).toString();
            ShipSummaryRowHeader()
                .stream()
                .forEach(s->header.add("攻撃艦"+index+"."+s));
        }
        header.add("雷撃");
        header.add("爆撃");
        header.add("クリティカル");
        header.add("ダメージ");
        header.add("かばう");
        ShipRowHeader()
            .stream()
            .forEach(s->header.add("防御艦."+s));
        return header;
    }

    private int[][] AirRowBodyConstruct(
            ArrayList<ArrayList<String>>enemyRows,
            ArrayList<ArrayList<String>>friendRows,
            ArrayList<ArrayList<String>>enemyCombinedRows,
            ArrayList<ArrayList<String>>combinedRows,
            ArrayList<ArrayList<String>>enemySummaryRows,
            ArrayList<ArrayList<String>>friendSummaryRows,
            AirBattleDto air,
            LinkedTreeMap api_kouku,
            int[][]startHP,
            ArrayList<ArrayList<String>> body,
            BuiltinScriptFilter filter)
        {
            int[][]prevHP = startHP;
            if(api_kouku.get("api_stage3")!=null || api_kouku.get("api_stage3_combined")!=null){
                ArrayList<String> rowHead = this.DayPhaseRowBodyAir(air);
                String stage1_f_count = "";
                String stage1_f_lostcount = "";
                String stage1_e_count = "";
                String stage1_e_lostcount = "";
                LinkedTreeMap api_stage1 = (LinkedTreeMap)api_kouku.get("api_stage1");
                if(api_stage1 != null){
                    stage1_f_count = GsonUtil.toIntString(api_stage1.get("api_f_count"));
                    stage1_f_lostcount = GsonUtil.toIntString(api_stage1.get("api_f_lostcount"));
                    stage1_e_count = GsonUtil.toIntString(api_stage1.get("api_e_count"));
                    stage1_e_lostcount = GsonUtil.toIntString(api_stage1.get("api_e_lostcount"));
                }
                rowHead.add(stage1_f_count);
                rowHead.add(stage1_f_lostcount);
                rowHead.add(stage1_e_count);
                rowHead.add(stage1_e_lostcount);
                String stage2_f_count = "";
                String stage2_f_lostcount = "";
                String stage2_e_count = "";
                String stage2_e_lostcount = "";
                String air_fire_idx = "";
                String air_fire_kind = "";
                String[] air_fire_use_item = new String[3];
                for(int i=0;i<air_fire_use_item.length;i++){ air_fire_use_item[i] = ""; }
                LinkedTreeMap api_stage2 = (LinkedTreeMap)api_kouku.get("api_stage2");
                if(api_stage2 != null){
                    stage2_f_count = GsonUtil.toIntString(api_stage2.get("api_f_count"));
                    stage2_f_lostcount = GsonUtil.toIntString(api_stage2.get("api_f_lostcount"));
                    stage2_e_count = GsonUtil.toIntString(api_stage2.get("api_e_count"));
                    stage2_e_lostcount = GsonUtil.toIntString(api_stage2.get("api_e_lostcount"));
                    LinkedTreeMap api_air_fire = (LinkedTreeMap)api_stage2.get("api_air_fire");
                    if(api_air_fire != null){
                        air_fire_idx = String.valueOf(GsonUtil.toInt(api_air_fire.get("api_idx"))+1);
                        air_fire_kind = GsonUtil.toIntString(api_air_fire.get("api_kind"));
                        int[] useItems = GsonUtil.toIntArray(api_air_fire.get("api_use_items"));
                        for(int i=0;i < air_fire_use_item.length && i < useItems.length;i++){
                            ItemInfoDto info = Item.get(useItems[i]);
                            if(info != null){ air_fire_use_item[i] = info.getName();}
                        }
                    }
                }
                rowHead.add(stage2_f_count);
                rowHead.add(stage2_f_lostcount);
                rowHead.add(stage2_e_count);
                rowHead.add(stage2_e_lostcount);
                rowHead.add(air_fire_idx);
                rowHead.add(air_fire_kind);
                rowHead.add(air_fire_use_item[0]);
                rowHead.add(air_fire_use_item[1]);
                rowHead.add(air_fire_use_item[2]);
                LinkedTreeMap api_stage3 = (LinkedTreeMap)api_kouku.get("api_stage3");
                if(api_stage3 != null){
                    int[] frai_flag = GsonUtil.toIntArray(api_stage3.get("api_frai_flag"));
                    int[] erai_flag = GsonUtil.toIntArray(api_stage3.get("api_erai_flag"));
                    int[] fbak_flag = GsonUtil.toIntArray(api_stage3.get("api_fbak_flag"));
                    int[] ebak_flag = GsonUtil.toIntArray(api_stage3.get("api_ebak_flag"));
                    int[] fcl_flag = GsonUtil.toIntArray(api_stage3.get("api_fcl_flag"));
                    int[] ecl_flag = GsonUtil.toIntArray(api_stage3.get("api_ecl_flag"));
                    double[] fdam = GsonUtil.toDoubleArray(api_stage3.get("api_fdam"));
                    double[] edam = GsonUtil.toDoubleArray(api_stage3.get("api_edam"));
                    for(int i=1; i<=6; i++){
                        int df = i;
                        ArrayList<String> row = (ArrayList<String>)rowHead.clone();
                        friendSummaryRows.stream().forEach(b->row.addAll(b));
                        row.add(String.valueOf(erai_flag[i]));
                        row.add(String.valueOf(ebak_flag[i]));
                        row.add(String.valueOf(ecl_flag[i]));
                        int damage = (int)edam[i];
                        boolean kabau = edam[i] - (double)damage > 0.05;
                        row.add(String.valueOf(damage));
                        row.add((kabau)?"1" :"0");
                        row.addAll((df-1)<this.maxEnemyHp.length?this.ShipRowBodyUpdate(enemyRows.get(df-1), prevHP[0][df-1],this.maxEnemyHp[df-1]) :enemyRows.get(df-1));
                        if(((df-1)<this.maxEnemyHp.length && filter.filterDefenceCountItem(this.getEnemy().get(df-1)))&&filter.filterOutput(row)){
                            body.add(row);
                        }
                    }
                    for(int i=1; i<=6; i++){
                        int df = i;
                        ArrayList<String> row = (ArrayList<String>)rowHead.clone();
                        enemySummaryRows.stream().forEach(b->row.addAll(b));
                        row.add(String.valueOf(frai_flag[i]));
                        row.add(String.valueOf(fbak_flag[i]));
                        row.add(String.valueOf(fcl_flag[i]));
                        int damage = (int)fdam[i];
                        boolean kabau = fdam[i] - (double)damage > 0.05;
                        row.add(String.valueOf(damage));
                        row.add((kabau)?"1" :"0");
                        row.addAll((df-1)<this.maxFriendHp.length?this.ShipRowBodyUpdate(friendRows.get(df-1), prevHP[1][df-1],this.maxFriendHp[df-1]) :friendRows.get(df-1));
                        if(((df-1)<this.maxFriendHp.length && filter.filterDefenceCountItem(this.getDock().getShips().get(df-1)))&&filter.filterOutput(row)){
                            body.add(row);
                        }
                    }
                }
                LinkedTreeMap combined = (LinkedTreeMap)api_kouku.get("api_stage3_combined");
                if(combined != null){
                    int[] frai_flag = GsonUtil.toIntArray(combined.get("api_frai_flag"));
                    int[] erai_flag = GsonUtil.toIntArray(combined.get("api_erai_flag"));
                    int[] fbak_flag = GsonUtil.toIntArray(combined.get("api_fbak_flag"));
                    int[] ebak_flag = GsonUtil.toIntArray(combined.get("api_ebak_flag"));
                    int[] fcl_flag = GsonUtil.toIntArray(combined.get("api_fcl_flag"));
                    int[] ecl_flag = GsonUtil.toIntArray(combined.get("api_ecl_flag"));
                    double[] fdam = GsonUtil.toDoubleArray(combined.get("api_fdam"));
                    double[] edam = GsonUtil.toDoubleArray(combined.get("api_edam"));
                    if(this.isCombined() && frai_flag != null){
                        for(int i=1; i<=6; i++){
                            int df = i;
                            ArrayList<String> row = (ArrayList<String>)rowHead.clone();
                            enemySummaryRows.stream().forEach(b->row.addAll(b));
                            row.add(String.valueOf(frai_flag[i]));
                            row.add(String.valueOf(fbak_flag[i]));
                            row.add(String.valueOf(fcl_flag[i]));
                            int damage = (int)fdam[i];
                            boolean kabau = fdam[i] - (double)damage > 0.05;
                            row.add(String.valueOf(damage));
                            row.add((kabau)?"1" :"0");
                            row.addAll((df-1)<this.maxFriendHpCombined.length?this.ShipRowBodyUpdate(combinedRows.get(df-1), prevHP[2][df-1],this.maxFriendHpCombined[df-1]) :combinedRows.get(df-1));
                            if(((df-1)<this.maxFriendHpCombined.length && filter.filterDefenceCountItem(this.getDockCombined().getShips().get(df-1)))&&filter.filterOutput(row)){
                                body.add(row);
                            }
                        }
                    }
                    if(this.isEnemyCombined() && erai_flag != null){
                        for(int i=1; i<=6; i++){
                            int df = i;
                            ArrayList<String> row = (ArrayList<String>)rowHead.clone();
                            friendSummaryRows.stream().forEach(b->row.addAll(b));
                            row.add(String.valueOf(erai_flag[i]));
                            row.add(String.valueOf(ebak_flag[i]));
                            row.add(String.valueOf(ecl_flag[i]));
                            int damage = (int)edam[i];
                            boolean kabau = edam[i] - (double)damage > 0.05;
                            row.add(String.valueOf(damage));
                            row.add((kabau)?"1" :"0");
                            row.addAll((df-1)<this.maxEnemyHpCombined.length?this.ShipRowBodyUpdate(enemyCombinedRows.get(df-1), prevHP[3][df-1],this.maxEnemyHpCombined[df-1]) :enemyCombinedRows.get(df-1));
                            if(((df-1)<this.maxEnemyHpCombined.length && filter.filterDefenceCountItem(this.getEnemyCombined().get(df-1)))&&filter.filterOutput(row)){
                                body.add(row);
                            }
                        }
                    }
                }
            }
            return createNextHPAir(prevHP, air);
        }

    private ArrayList<ArrayList<String>>AirRowBody(Phase phase,LinkedTreeMap tree,ArrayList<ArrayList<String>> enemyRows,ArrayList<ArrayList<String>> friendRows,ArrayList<ArrayList<String>> enemyCombinedRows,ArrayList<ArrayList<String>> combinedRows,BuiltinScriptFilter filter){
        ArrayList<ArrayList<String>> body = new ArrayList<ArrayList<String>>();
        if(phase.isNight){
            return body;
        }
        if(enemyRows == null){
            enemyRows = new ArrayList<ArrayList<String>>();
            for(int i=0;i<this.enemy.size();i++){ enemyRows.add(this.ShipRowBodyBase(this.enemy.get(i), this.maxEnemyHp[i], i));}
            for(int i=this.enemy.size();i<6;i++){ enemyRows.add(this.ShipRowBodyBase(null,0, i));}
        }
        if(friendRows == null){
            friendRows = new ArrayList<ArrayList<String>>();
            if(this.getDock()!=null){
                List<ShipDto> ships = this.getDock().getShips();
                for(int i=0;i<ships.size();i++){ friendRows.add(this.ShipRowBodyBase(ships.get(i), this.maxFriendHp[i], i));}
                for(int i=ships.size();i<6;i++){ friendRows.add(this.ShipRowBodyBase(null, 0, i));}
            }else{
                for(int i=0;i<6;i++){ friendRows.add(this.ShipRowBodyBase(null, 0, i));}
            }
        }
        if(combinedRows == null){
            combinedRows = new ArrayList<ArrayList<String>>();
            if(this.isCombined() && this.getDockCombined()!=null){
                List<ShipDto> ships = this.getDockCombined().getShips();
                for(int i=0;i<ships.size();i++){ combinedRows.add(this.ShipRowBodyBase(ships.get(i), this.maxFriendHpCombined[i], i+6));}
                for(int i=ships.size();i<6;i++){ combinedRows.add(this.ShipRowBodyBase(null, 0, i+6));}
            }else{
                for(int i=0;i<6;i++){ combinedRows.add(this.ShipRowBodyBase(null, 0, i+6));}
            }
        }
        if(enemyCombinedRows == null){
            enemyCombinedRows = new ArrayList<ArrayList<String>>();
            if(this.isEnemyCombined()){
                for(int i=0;i<this.enemyCombined.size();i++){ enemyCombinedRows.add(this.ShipRowBodyBase(this.enemyCombined.get(i), this.maxEnemyHpCombined[i], i+6));}
                for(int i=this.enemyCombined.size();i<6;i++){ enemyCombinedRows.add(this.ShipRowBodyBase(null, 0, i+6));}
            }else{
                for(int i=0;i<6;i++){ enemyCombinedRows.add(this.ShipRowBodyBase(null, 0, i+6));}
            }
        }
        ArrayList<ArrayList<String>> enemySummaryRows = new ArrayList<ArrayList<String>>();
        for(int i=0;i<this.enemy.size();i++){enemySummaryRows.add(this.ShipSummaryRowBody(this.enemy.get(i)));}
        for(int i=this.enemy.size();i<6;i++){enemySummaryRows.add(this.ShipSummaryRowBody(null));}
        ArrayList<ArrayList<String>> friendSummaryRows = new ArrayList<ArrayList<String>>();
        if(this.getDock()!=null){
            List<ShipDto> ships = this.getDock().getShips();
            for(int i=0;i<ships.size();i++){friendSummaryRows.add(this.ShipSummaryRowBody(ships.get(i)));}
            for(int i=ships.size();i<6;i++){friendSummaryRows.add(this.ShipSummaryRowBody(null));}
        }
        int combinedFlag = this.getCombinedKind();
        String combinedFlagString =
            (combinedFlag == 0)?"通常艦隊":
            (combinedFlag == 1)?"機動部隊":
            (combinedFlag == 2)?"水上部隊":
            (combinedFlag == 3)?"輸送部隊":
            "不明";

        int[][]phaseStartHP = new int[4][];
        if(phase == this.getPhase1()){
            phaseStartHP[0] = this.startEnemyHp.clone();
            phaseStartHP[1] = this.startFriendHp.clone();
            phaseStartHP[2] = (this.isCombined()) ?this.startFriendHpCombined.clone() :null;
            phaseStartHP[3] = (this.isEnemyCombined()) ? this.startEnemyHpCombined.clone() : null;
        }
        else{
            Phase phase1 = this.getPhase1();
            phaseStartHP[0] = phase1.nowEnemyHp.clone();
            phaseStartHP[1] = phase1.nowFriendHp.clone();
            phaseStartHP[2] = (this.isCombined()) ?phase1.nowFriendHpCombined.clone() :null;
            phaseStartHP[3] = (this.isEnemyCombined()) ?phase1.nowEnemyHpCombined.clone() : null;
        }
        int[][]injectionAirBaseStartHP = phaseStartHP;
        int[][]injectionAirStartHP = this.createNextHPAir(injectionAirBaseStartHP, phase.airBaseInjection);
        int[][]airBaseStartHP = injectionAirStartHP;
        if(phase.airInjection != null){
            LinkedTreeMap api_kouku = (LinkedTreeMap)tree.get("api_injection_kouku");
            airBaseStartHP =
                this.AirRowBodyConstruct(
                    enemyRows,
                    friendRows,
                    enemyCombinedRows,
                    combinedRows,
                    enemySummaryRows,
                    friendSummaryRows,
                    phase.airInjection,
                    api_kouku,
                    injectionAirStartHP,
                    body,
                    filter
                );
        }
        int[][]air1StartHP = airBaseStartHP;
        if(phase.airBase != null){
            ArrayList<int[][]>airHP = this.createNextHPAirBase(airBaseStartHP,phase.airBase);
            air1StartHP = airHP.get(airHP.size()-1);
        }
        int[][]supportStartHP = air1StartHP;
        if(phase.air != null){
            LinkedTreeMap api_kouku = (LinkedTreeMap)tree.get("api_kouku");
            supportStartHP =
                this.AirRowBodyConstruct(
                    enemyRows,
                    friendRows,
                    enemyCombinedRows,
                    combinedRows,
                    enemySummaryRows,
                    friendSummaryRows,
                    phase.air,
                    api_kouku,
                    air1StartHP,
                    body,
                    filter
                );
        }
        int[][]openingTaisenStartHP = this.createNextHP(supportStartHP,phase.support);
        int[][]openingRaigekiStartHP = this.createNextHP(openingTaisenStartHP,phase.openingTaisen);
        int[][]air2StartHP = this.createNextHP(openingRaigekiStartHP, phase.opening);
        int[][]hougeki1StartHP = air2StartHP;
        if(phase.air2 != null){
            LinkedTreeMap api_kouku = (LinkedTreeMap)tree.get("api_kouku2");
            hougeki1StartHP =
                this.AirRowBodyConstruct(
                        enemyRows,
                        friendRows,
                        enemyCombinedRows,
                        combinedRows,
                        enemySummaryRows,
                        friendSummaryRows,
                        phase.air2,
                        api_kouku,
                        air2StartHP,
                        body,
                        filter
                    );
        }

        int[][]hougeki2StartHP;
        int[][]hougeki3StartHP;
        int[][]raigekiStartHP;
        int[][]endHP;
        if(phase.getKind().isHougeki1Second()){
            raigekiStartHP = createNextHP(hougeki1StartHP,phase.hougeki1);
            hougeki2StartHP = createNextHP(raigekiStartHP,phase.raigeki);
            hougeki3StartHP = createNextHP(hougeki2StartHP, phase.hougeki2);
            endHP = createNextHP(hougeki3StartHP,phase.hougeki3);
        }else{
            hougeki2StartHP = createNextHP(hougeki1StartHP,phase.hougeki1);
            hougeki3StartHP = createNextHP(hougeki2StartHP,phase.hougeki2);
            raigekiStartHP = createNextHP(hougeki3StartHP,phase.hougeki3);
            endHP =createNextHP(raigekiStartHP,phase.raigeki);
        }
        return body;
    }

    static public ArrayList<String> BaseAirRowHeader(){
        ArrayList<String> header = DayPhaseRowHeader();
        header.add("航空隊");
        header.add("攻撃順");
        header.add("基地自触接");
        header.add("基地敵触接");
        for(int i=1;i<=4;i++){
            String index = (new Integer(i)).toString();
            header.add("第"+index+"中隊");
            header.add("第"+index+"機数");
        }
        header.add("ステージ1.自艦載機総数");
        header.add("ステージ1.自艦載機喪失数");
        header.add("ステージ1.敵艦載機総数");
        header.add("ステージ1.敵艦載機喪失数");
        header.add("ステージ2.自艦載機総数");
        header.add("ステージ2.自艦載機喪失数");
        header.add("ステージ2.敵艦載機総数");
        header.add("ステージ2.敵艦載機喪失数");

        for(int i=1;i <= 6; ++i){
            String index = (new Integer(i)).toString();
            ShipSummaryRowHeader()
                .stream()
                .forEach(s->header.add("攻撃艦"+index+"."+s));
        }
        header.add("雷撃");
        header.add("爆撃");
        header.add("クリティカル");
        header.add("ダメージ");
        header.add("かばう");
        ShipRowHeader()
            .stream()
            .forEach(s->header.add("防御艦."+s));
        return header;
    }

    private int[][] BaseAirRowBodyConstruct(
            ArrayList<ArrayList<String>>enemyRows,
            ArrayList<ArrayList<String>>enemyCombinedRows,
            ArrayList<ArrayList<String>>friendSummaryRows,
            AirBattleDto air,
            LinkedTreeMap api_kouku,
            int airIndex,
            int[][]startHP,
            ArrayList<ArrayList<String>> body,
            BuiltinScriptFilter filter)
        {
            int[][]prevHP = startHP;
            if(api_kouku.get("api_stage3")!=null){
                ArrayList<String> rowHead = this.DayPhaseRowBodyAir(air);
                rowHead.add(GsonUtil.toIntString(api_kouku.get("api_base_id")));
                rowHead.add(String.valueOf(airIndex));

                String touch_plane0 = "";
                String touch_plane1 = "";
                LinkedTreeMap api_stage1 = (LinkedTreeMap)api_kouku.get("api_stage1");
                if(api_stage1!=null){
                    int[] touch_plane = GsonUtil.toIntArray(api_stage1.get("api_touch_plane"));
                    if(touch_plane!=null){
                        ItemInfoDto info0 = Item.get(touch_plane[0]);
                        if(info0!=null){touch_plane0 = info0.getName();}
                        ItemInfoDto info1 = Item.get(touch_plane[1]);
                        if(info1!=null){touch_plane1 = info1.getName();}
                    }
                }
                rowHead.add(touch_plane0);
                rowHead.add(touch_plane1);
                List<Object> squadron_plane = (List<Object>)api_kouku.get("api_squadron_plane");
                for(int i=0;i<4;++i){
                    String basePlane = "";
                    String count = "";
                    if(squadron_plane != null && i < squadron_plane.size()){
                        LinkedTreeMap plane = (LinkedTreeMap)squadron_plane.get(i);
                        ItemInfoDto info = Item.get(GsonUtil.toInt(plane.get("api_mst_id")));
                        if(info != null){ basePlane = info.getName(); }
                        count = GsonUtil.toIntString(plane.get("api_count"));
                    }
                    rowHead.add(basePlane);
                    rowHead.add(count);
                }

                String stage1_f_count = "";
                String stage1_f_lostcount = "";
                String stage1_e_count = "";
                String stage1_e_lostcount = "";
                if(api_stage1 != null){
                    stage1_f_count = GsonUtil.toIntString(api_stage1.get("api_f_count"));
                    stage1_f_lostcount = GsonUtil.toIntString(api_stage1.get("api_f_lostcount"));
                    stage1_e_count = GsonUtil.toIntString(api_stage1.get("api_e_count"));
                    stage1_e_lostcount = GsonUtil.toIntString(api_stage1.get("api_e_lostcount"));
                }
                rowHead.add(stage1_f_count);
                rowHead.add(stage1_f_lostcount);
                rowHead.add(stage1_e_count);
                rowHead.add(stage1_e_lostcount);
                String stage2_f_count = "";
                String stage2_f_lostcount = "";
                String stage2_e_count = "";
                String stage2_e_lostcount = "";
                LinkedTreeMap api_stage2 = (LinkedTreeMap)api_kouku.get("api_stage2");
                if(api_stage2!=null){
                    stage2_f_count = GsonUtil.toIntString(api_stage2.get("api_f_count"));
                    stage2_f_lostcount = GsonUtil.toIntString(api_stage2.get("api_f_lostcount"));
                    stage2_e_count = GsonUtil.toIntString(api_stage2.get("api_e_count"));
                    stage2_e_lostcount = GsonUtil.toIntString(api_stage2.get("api_e_lostcount"));
                }
                rowHead.add(stage2_f_count);
                rowHead.add(stage2_f_lostcount);
                rowHead.add(stage2_e_count);
                rowHead.add(stage2_e_lostcount);
                LinkedTreeMap api_stage3 = (LinkedTreeMap)api_kouku.get("api_stage3");
                if(api_stage3 != null){
                    int[] erai_flag = GsonUtil.toIntArray(api_stage3.get("api_erai_flag"));
                    int[] ebak_flag = GsonUtil.toIntArray(api_stage3.get("api_ebak_flag"));
                    int[] ecl_flag = GsonUtil.toIntArray(api_stage3.get("api_ecl_flag"));
                    double[] edam = GsonUtil.toDoubleArray(api_stage3.get("api_edam"));
                    for(int i=1; i<=6; i++){
                        int df = i;
                        ArrayList<String> row = (ArrayList<String>)rowHead.clone();
                        friendSummaryRows.stream().forEach(b->row.addAll(b));
                        row.add(String.valueOf(erai_flag[i]));
                        row.add(String.valueOf(ebak_flag[i]));
                        row.add(String.valueOf(ecl_flag[i]));
                        int damage = (int)edam[i];
                        boolean kabau = edam[i] - (double)damage > 0.05;
                        row.add(String.valueOf(damage));
                        row.add((kabau)?"1" :"0");
                        row.addAll((df-1)<this.maxEnemyHp.length?this.ShipRowBodyUpdate(enemyRows.get(df-1), prevHP[0][df-1],this.maxEnemyHp[df-1]):enemyRows.get(df-1));
                        if((df-1)<this.maxEnemyHp.length && filter.filterDefenceCountItem(this.enemy.get(df-1)) && filter.filterOutput(row)){
                            body.add(row);
                        }
                    }
                }
                LinkedTreeMap combined = (LinkedTreeMap)api_kouku.get("api_stage3_combined");
                if(combined != null){
                    int[] erai_flag = GsonUtil.toIntArray(combined.get("api_erai_flag"));
                    int[] ebak_flag = GsonUtil.toIntArray(combined.get("api_ebak_flag"));
                    int[] ecl_flag = GsonUtil.toIntArray(combined.get("api_ecl_flag"));
                    double[] edam = GsonUtil.toDoubleArray(combined.get("api_edam"));
                    for(int i=1; i<=6; i++){
                        int df = i;
                        ArrayList<String> row = (ArrayList<String>)rowHead.clone();
                        friendSummaryRows.stream().forEach(b->row.addAll(b));
                        row.add(String.valueOf(erai_flag[i]));
                        row.add(String.valueOf(ebak_flag[i]));
                        row.add(String.valueOf(ecl_flag[i]));
                        int damage = (int)edam[i];
                        boolean kabau = edam[i] - (double)damage > 0.05;
                        row.add(String.valueOf(damage));
                        row.add((kabau)?"1" :"0");
                        row.addAll((df-1)<this.maxEnemyHpCombined.length?this.ShipRowBodyUpdate(enemyCombinedRows.get(df-1), prevHP[3][df-1],this.maxEnemyHpCombined[df-1]):enemyCombinedRows.get(df-1));
                        if((df-1)<this.maxEnemyHpCombined.length && filter.filterDefenceCountItem(this.enemyCombined.get(df-1)) && filter.filterOutput(row)){
                            body.add(row);
                        }
                    }
                }
            }
            return createNextHPAir(prevHP, air);
        }

    private ArrayList<ArrayList<String>>BaseAirRowBody(Phase phase,LinkedTreeMap tree,ArrayList<ArrayList<String>> enemyRows,ArrayList<ArrayList<String>> friendRows,ArrayList<ArrayList<String>> enemyCombinedRows,ArrayList<ArrayList<String>> combinedRows,BuiltinScriptFilter filter){
        ArrayList<ArrayList<String>> body = new ArrayList<ArrayList<String>>();
        if(phase.isNight){
            return body;
        }
        if(enemyRows == null){
            enemyRows = new ArrayList<ArrayList<String>>();
            for(int i=0;i<this.enemy.size();i++){ enemyRows.add(this.ShipRowBodyBase(this.enemy.get(i), this.maxEnemyHp[i], i));}
            for(int i=this.enemy.size();i<6;i++){ enemyRows.add(this.ShipRowBodyBase(null,0, i));}
        }
        if(enemyCombinedRows == null){
            enemyCombinedRows = new ArrayList<ArrayList<String>>();
            if(this.isEnemyCombined()){
                for(int i=0;i<this.enemyCombined.size();i++){ enemyCombinedRows.add(this.ShipRowBodyBase(this.enemyCombined.get(i), this.maxEnemyHpCombined[i], i+6));}
                for(int i=this.enemyCombined.size();i<6;i++){ enemyCombinedRows.add(this.ShipRowBodyBase(null, 0, i+6));}
            }else{
                for(int i=0;i<6;i++){ enemyCombinedRows.add(this.ShipRowBodyBase(null, 0, i+6));}
            }
        }
        ArrayList<ArrayList<String>> friendSummaryRows = new ArrayList<ArrayList<String>>();
        if(this.getDock()!=null){
            List<ShipDto> ships = this.getDock().getShips();
            for(int i=0;i<ships.size();i++){friendSummaryRows.add(this.ShipSummaryRowBody(ships.get(i)));}
            for(int i=ships.size();i<6;i++){friendSummaryRows.add(this.ShipSummaryRowBody(null));}
        }
        int combinedFlag = this.getCombinedKind();
        String combinedFlagString =
            (combinedFlag == 0)?"通常艦隊":
            (combinedFlag == 1)?"機動部隊":
            (combinedFlag == 2)?"水上部隊":
            (combinedFlag == 3)?"輸送部隊":
            "不明";

        int[][]phaseStartHP = new int[4][];
        if(phase == this.getPhase1()){
            phaseStartHP[0] = this.startEnemyHp.clone();
            phaseStartHP[1] = this.startFriendHp.clone();
            phaseStartHP[2] = (this.isCombined()) ?this.startFriendHpCombined.clone() :null;
            phaseStartHP[3] = (this.isEnemyCombined()) ? this.startEnemyHpCombined.clone() : null;
        }
        else{
            Phase phase1 = this.getPhase1();
            phaseStartHP[0] = phase1.nowEnemyHp.clone();
            phaseStartHP[1] = phase1.nowFriendHp.clone();
            phaseStartHP[2] = (this.isCombined()) ?phase1.nowFriendHpCombined.clone() :null;
            phaseStartHP[3] = (this.isEnemyCombined()) ?phase1.nowEnemyHpCombined.clone() : null;
        }
        int[][]injectionAirBaseStartHP = phaseStartHP;
        int[][]injectionAirStartHP = this.createNextHPAir(injectionAirBaseStartHP, phase.airBaseInjection);
        int[][]airBaseStartHP = this.createNextHPAir(injectionAirStartHP, phase.airInjection);
        int[][]air1StartHP = airBaseStartHP;
        if(phase.airBase != null){
            List<Object> airBase = (List<Object>)tree.get("api_air_base_attack");
            int[][] prevHP = airBaseStartHP;
            for(int i=0;i<phase.airBase.size();i++){
                AirBattleDto air = phase.airBase.get(i);
                LinkedTreeMap api_kouku = (LinkedTreeMap)airBase.get(i);
                prevHP =
                    this.BaseAirRowBodyConstruct(
                        enemyRows,
                        enemyCombinedRows,
                        friendSummaryRows,
                        air,
                        api_kouku,
                        i+1,
                        prevHP,
                        body,
                        filter
                    );
            }
            air1StartHP = prevHP;
        }
        int[][]supportStartHP = createNextHPAir(air1StartHP,phase.air);
        int[][]openingTaisenStartHP = this.createNextHP(supportStartHP,phase.support);
        int[][]openingRaigekiStartHP = this.createNextHP(openingTaisenStartHP,phase.openingTaisen);
        int[][]air2StartHP = this.createNextHP(openingRaigekiStartHP, phase.opening);
        int[][]hougeki1StartHP = this.createNextHPAir(air2StartHP, phase.air2);

        int[][]hougeki2StartHP;
        int[][]hougeki3StartHP;
        int[][]raigekiStartHP;
        int[][]endHP;
        if(phase.getKind().isHougeki1Second()){
            raigekiStartHP = createNextHP(hougeki1StartHP,phase.hougeki1);
            hougeki2StartHP = createNextHP(raigekiStartHP,phase.raigeki);
            hougeki3StartHP = createNextHP(hougeki2StartHP, phase.hougeki2);
            endHP = createNextHP(hougeki3StartHP,phase.hougeki3);
        }else{
            hougeki2StartHP = createNextHP(hougeki1StartHP,phase.hougeki1);
            hougeki3StartHP = createNextHP(hougeki2StartHP,phase.hougeki2);
            raigekiStartHP = createNextHP(hougeki3StartHP,phase.hougeki3);
            endHP =createNextHP(raigekiStartHP,phase.raigeki);
        }
        return body;
    }

    static public ArrayList<String> AirLostRowHeader(){
        ArrayList<String> header = DayPhaseRowHeader();
        header.add("ステージ1.自艦載機総数");
        header.add("ステージ1.自艦載機喪失数");
        header.add("ステージ1.敵艦載機総数");
        header.add("ステージ1.敵艦載機喪失数");
        header.add("ステージ2.自艦載機総数");
        header.add("ステージ2.自艦載機喪失数");
        header.add("ステージ2.敵艦載機総数");
        header.add("ステージ2.敵艦載機喪失数");
        header.add("対空カットイン.発動艦");
        header.add("対空カットイン.種別");
        header.add("対空カットイン.表示装備1");
        header.add("対空カットイン.表示装備2");
        header.add("対空カットイン.表示装備3");
        header.add("味方雷撃被タゲ数");
        header.add("味方爆撃被タゲ数");
        header.add("敵雷撃被タゲ数");
        header.add("敵爆撃被タゲ数");
        for(int i=1;i <= 6; ++i){
            String index = (new Integer(i)).toString();
            ShipRowHeader()
                .stream()
                .forEach(s->header.add("敵艦"+index+"."+s));
        }
        for(int i=1;i <= 6; ++i){
            String index = (new Integer(i)).toString();
            ShipRowHeader()
                .stream()
                .forEach(s->header.add("味方艦"+index+"."+s));
        }
        for(int i=1;i <= 6; ++i){
            String index = (new Integer(i)).toString();
            ShipRowHeader()
                .stream()
                .forEach(s->header.add("敵連合第二艦隊"+index+"."+s));
        }
        for(int i=1;i <= 6; ++i){
            String index = (new Integer(i)).toString();
            ShipRowHeader()
                .stream()
                .forEach(s->header.add("味方連合第二艦隊"+index+"."+s));
        }
        header.add("艦隊種類");
        header.add("敵艦隊種類");
        return header;
    }

    private int[][] AirLostRowBodyConstruct(
            ArrayList<ArrayList<String>>enemyRows,
            ArrayList<ArrayList<String>>friendRows,
            ArrayList<ArrayList<String>>enemyCombinedRows,
            ArrayList<ArrayList<String>>combinedRows,
            AirBattleDto air,
            LinkedTreeMap api_kouku,
            String combinedFlagString,
            int[][]startHP,
            ArrayList<ArrayList<String>> body,
            BuiltinScriptFilter filter)
        {
            int[][]prevHP = startHP;
            ArrayList<String> row = this.DayPhaseRowBodyAir(air);
            String stage1_f_count = "";
            String stage1_f_lostcount = "";
            String stage1_e_count = "";
            String stage1_e_lostcount = "";
            LinkedTreeMap api_stage1 = (LinkedTreeMap)api_kouku.get("api_stage1");
            if(api_stage1!=null){
                stage1_f_count = GsonUtil.toIntString(api_stage1.get("api_f_count"));
                stage1_f_lostcount = GsonUtil.toIntString(api_stage1.get("api_f_lostcount"));
                stage1_e_count = GsonUtil.toIntString(api_stage1.get("api_e_count"));
                stage1_e_lostcount = GsonUtil.toIntString(api_stage1.get("api_e_lostcount"));
                if(GsonUtil.toInt(api_stage1.get("api_f_count"))==0 && GsonUtil.toInt(api_stage1.get("api_e_count"))==0){
                    return createNextHPAir(prevHP, air);
                }
            }
            row.add(stage1_f_count);
            row.add(stage1_f_lostcount);
            row.add(stage1_e_count);
            row.add(stage1_e_lostcount);
            String stage2_f_count = "";
            String stage2_f_lostcount = "";
            String stage2_e_count = "";
            String stage2_e_lostcount = "";
            String air_fire_idx = "";
            String air_fire_kind = "";
            String[] air_fire_use_item = new String[3];
            for(int i=0;i<air_fire_use_item.length;i++){ air_fire_use_item[i] = ""; }
            LinkedTreeMap api_stage2 = (LinkedTreeMap)api_kouku.get("api_stage2");
            if(api_stage2 != null){
                stage2_f_count = GsonUtil.toIntString(api_stage2.get("api_f_count"));
                stage2_f_lostcount = GsonUtil.toIntString(api_stage2.get("api_f_lostcount"));
                stage2_e_count = GsonUtil.toIntString(api_stage2.get("api_e_count"));
                stage2_e_lostcount = GsonUtil.toIntString(api_stage2.get("api_e_lostcount"));
                LinkedTreeMap api_air_fire = (LinkedTreeMap)api_stage2.get("api_air_fire");
                if(api_air_fire != null){
                    air_fire_idx = String.valueOf(GsonUtil.toInt(api_air_fire.get("api_idx"))+1);
                    air_fire_kind = GsonUtil.toIntString(api_air_fire.get("api_kind"));
                    int[] useItems = GsonUtil.toIntArray(api_air_fire.get("api_use_items"));
                    for(int i=0;i<air_fire_use_item.length && i<useItems.length;i++){
                        ItemInfoDto info = Item.get(useItems[i]);
                        if(info != null){ air_fire_use_item[i] = info.getName();}
                    }
                }
            }
            row.add(stage2_f_count);
            row.add(stage2_f_lostcount);
            row.add(stage2_e_count);
            row.add(stage2_e_lostcount);
            row.add(air_fire_idx);
            row.add(air_fire_kind);
            row.add(air_fire_use_item[0]);
            row.add(air_fire_use_item[1]);
            row.add(air_fire_use_item[2]);
            int frai_count = 0;
            int erai_count = 0;
            int fbak_count = 0;
            int ebak_count = 0;
            LinkedTreeMap api_stage3 = (LinkedTreeMap)api_kouku.get("api_stage3");
            if(api_stage3 != null){
                int[] frai_flag = GsonUtil.toIntArray(api_stage3.get("api_frai_flag"));
                int[] erai_flag = GsonUtil.toIntArray(api_stage3.get("api_erai_flag"));
                int[] fbak_flag = GsonUtil.toIntArray(api_stage3.get("api_fbak_flag"));
                int[] ebak_flag = GsonUtil.toIntArray(api_stage3.get("api_ebak_flag"));
                for(int i=0;i<frai_flag.length;i++){
                    if(frai_flag[i]==1){frai_count++;}
                    if(erai_flag[i]==1){erai_count++;}
                    if(fbak_flag[i]==1){fbak_count++;}
                    if(ebak_flag[i]==1){ebak_count++;}
                }
            }
            LinkedTreeMap combined = (LinkedTreeMap)api_kouku.get("api_stage3_combined");
            if(combined != null){
                int[] frai_flag = GsonUtil.toIntArray(combined.get("api_frai_flag"));
                int[] fbak_flag = GsonUtil.toIntArray(combined.get("api_fbak_flag"));
                int[] erai_flag = GsonUtil.toIntArray(combined.get("api_erai_flag"));
                int[] ebak_flag = GsonUtil.toIntArray(combined.get("api_ebak_flag"));
                for(int i=0;i<frai_flag.length;i++){
                    if(frai_flag[i]==1){frai_count++;}
                    if(fbak_flag[i]==1){fbak_count++;}
                }
                if(erai_flag!=null){
                    for(int i=0;i<erai_flag.length;i++){
                        if(erai_flag[i]==1){erai_count++;}
                        if(ebak_flag[i]==1){ebak_count++;}
                    }
                }
            }
            row.add(String.valueOf(frai_count));
            row.add(String.valueOf(fbak_count));
            row.add(String.valueOf(erai_count));
            row.add(String.valueOf(ebak_count));
            for (int i = 0; i < 6; ++i) { row.addAll(i<this.maxEnemyHp.length?this.ShipRowBodyUpdate(enemyRows.get(i),prevHP[0][i],this.maxEnemyHp[i]):enemyRows.get(i)); }
            for (int i = 0; i < 6; ++i) { row.addAll(i<this.maxFriendHp.length?this.ShipRowBodyUpdate(friendRows.get(i),prevHP[1][i],this.maxFriendHp[i]):friendRows.get(i)); }
            for (int i = 0; i < 6; ++i) { row.addAll((this.isEnemyCombined() && i<this.maxEnemyHpCombined.length)?this.ShipRowBodyUpdate(enemyCombinedRows.get(i),prevHP[3][i],this.maxEnemyHpCombined[i]):enemyCombinedRows.get(i)); }
            for (int i = 0; i < 6; ++i) { row.addAll((this.isCombined() && i<this.maxFriendHpCombined.length)?this.ShipRowBodyUpdate(combinedRows.get(i),prevHP[2][i],this.maxFriendHpCombined[i]):combinedRows.get(i)); }
            row.add(combinedFlagString);
            row.add(isEnemyCombined()?"連合艦隊":"通常艦隊");
            if(filter.filterOutput(row)){
                body.add(row);
            }
            return createNextHPAir(prevHP, air);
        }

    private ArrayList<ArrayList<String>>AirLostRowBody(Phase phase,LinkedTreeMap tree,ArrayList<ArrayList<String>> enemyRows,ArrayList<ArrayList<String>> friendRows,ArrayList<ArrayList<String>> enemyCombinedRows,ArrayList<ArrayList<String>> combinedRows,BuiltinScriptFilter filter){
        ArrayList<ArrayList<String>> body = new ArrayList<ArrayList<String>>();
        if(phase.isNight){
            return body;
        }
        if(enemyRows == null){
            enemyRows = new ArrayList<ArrayList<String>>();
            for(int i=0;i<this.enemy.size();i++){ enemyRows.add(this.ShipRowBodyBase(this.enemy.get(i), this.maxEnemyHp[i], i));}
            for(int i=this.enemy.size();i<6;i++){ enemyRows.add(this.ShipRowBodyBase(null,0, i));}
        }
        if(friendRows == null){
            friendRows = new ArrayList<ArrayList<String>>();
            if(this.getDock()!=null){
                List<ShipDto> ships = this.getDock().getShips();
                for(int i=0;i<ships.size();i++){ friendRows.add(this.ShipRowBodyBase(ships.get(i), this.maxFriendHp[i], i));}
                for(int i=ships.size();i<6;i++){ friendRows.add(this.ShipRowBodyBase(null, 0, i));}
            }else{
                for(int i=0;i<6;i++){ friendRows.add(this.ShipRowBodyBase(null, 0, i));}
            }
        }
        if(combinedRows == null){
            combinedRows = new ArrayList<ArrayList<String>>();
            if(this.isCombined() && this.getDockCombined()!=null){
                List<ShipDto> ships = this.getDockCombined().getShips();
                for(int i=0;i<ships.size();i++){ combinedRows.add(this.ShipRowBodyBase(ships.get(i), this.maxFriendHpCombined[i], i+6));}
                for(int i=ships.size();i<6;i++){ combinedRows.add(this.ShipRowBodyBase(null, 0, i+6));}
            }else{
                for(int i=0;i<6;i++){ combinedRows.add(this.ShipRowBodyBase(null, 0, i+6));}
            }
        }
        if(enemyCombinedRows == null){
            enemyCombinedRows = new ArrayList<ArrayList<String>>();
            if(this.isEnemyCombined()){
                for(int i=0;i<this.enemyCombined.size();i++){ enemyCombinedRows.add(this.ShipRowBodyBase(this.enemyCombined.get(i), this.maxEnemyHpCombined[i], i+6));}
                for(int i=this.enemyCombined.size();i<6;i++){ enemyCombinedRows.add(this.ShipRowBodyBase(null, 0, i+6));}
            }else{
                for(int i=0;i<6;i++){ enemyCombinedRows.add(this.ShipRowBodyBase(null, 0, i+6));}
            }
        }
        int combinedFlag = this.getCombinedKind();
        String combinedFlagString =
            (combinedFlag == 0)?"通常艦隊":
            (combinedFlag == 1)?"機動部隊":
            (combinedFlag == 2)?"水上部隊":
            (combinedFlag == 3)?"輸送部隊":
            "不明";

        int[][]phaseStartHP = new int[4][];
        if(phase == this.getPhase1()){
            phaseStartHP[0] = this.startEnemyHp.clone();
            phaseStartHP[1] = this.startFriendHp.clone();
            phaseStartHP[2] = (this.isCombined()) ?this.startFriendHpCombined.clone() :null;
            phaseStartHP[3] = (this.isEnemyCombined()) ? this.startEnemyHpCombined.clone() : null;
        }
        else{
            Phase phase1 = this.getPhase1();
            phaseStartHP[0] = phase1.nowEnemyHp.clone();
            phaseStartHP[1] = phase1.nowFriendHp.clone();
            phaseStartHP[2] = (this.isCombined()) ?phase1.nowFriendHpCombined.clone() :null;
            phaseStartHP[3] = (this.isEnemyCombined()) ?phase1.nowEnemyHpCombined.clone() : null;
        }
        int[][]injectionAirBaseStartHP = phaseStartHP;
        int[][]injectionAirStartHP = this.createNextHPAir(injectionAirBaseStartHP, phase.airBaseInjection);
        int[][]airBaseStartHP = injectionAirStartHP;
        if(phase.airInjection != null){
            LinkedTreeMap api_kouku = (LinkedTreeMap)tree.get("api_injection_kouku");
            airBaseStartHP =
                this.AirLostRowBodyConstruct(
                    enemyRows,
                    friendRows,
                    enemyCombinedRows,
                    combinedRows,
                    phase.airInjection,
                    api_kouku,
                    combinedFlagString,
                    injectionAirStartHP,
                    body,
                    filter
                );
        }
        int[][]air1StartHP = airBaseStartHP;
        if(phase.airBase != null){
            ArrayList<int[][]>airHP = this.createNextHPAirBase(airBaseStartHP,phase.airBase);
            air1StartHP = airHP.get(airHP.size()-1);
        }
        int[][]supportStartHP = air1StartHP;
        if(phase.air != null){
            LinkedTreeMap api_kouku = (LinkedTreeMap)tree.get("api_kouku");
            supportStartHP =
                this.AirLostRowBodyConstruct(
                    enemyRows,
                    friendRows,
                    enemyCombinedRows,
                    combinedRows,
                    phase.air,
                    api_kouku,
                    combinedFlagString,
                    air1StartHP,
                    body,
                    filter
                );
        }
        int[][]openingTaisenStartHP = this.createNextHP(supportStartHP,phase.support);
        int[][]openingRaigekiStartHP = this.createNextHP(openingTaisenStartHP,phase.openingTaisen);
        int[][]air2StartHP = this.createNextHP(openingRaigekiStartHP, phase.opening);
        int[][]hougeki1StartHP = air2StartHP;
        if(phase.air2 != null){
            LinkedTreeMap api_kouku = (LinkedTreeMap)tree.get("api_kouku2");
            hougeki1StartHP =
                this.AirLostRowBodyConstruct(
                        enemyRows,
                        friendRows,
                        enemyCombinedRows,
                        combinedRows,
                        phase.air2,
                        api_kouku,
                        combinedFlagString,
                        air2StartHP,
                        body,
                        filter
                    );
        }

        int[][]hougeki2StartHP;
        int[][]hougeki3StartHP;
        int[][]raigekiStartHP;
        int[][]endHP;
        if(phase.getKind().isHougeki1Second()){
            raigekiStartHP = createNextHP(hougeki1StartHP,phase.hougeki1);
            hougeki2StartHP = createNextHP(raigekiStartHP,phase.raigeki);
            hougeki3StartHP = createNextHP(hougeki2StartHP, phase.hougeki2);
            endHP = createNextHP(hougeki3StartHP,phase.hougeki3);
        }else{
            hougeki2StartHP = createNextHP(hougeki1StartHP,phase.hougeki1);
            hougeki3StartHP = createNextHP(hougeki2StartHP,phase.hougeki2);
            raigekiStartHP = createNextHP(hougeki3StartHP,phase.hougeki3);
            endHP =createNextHP(raigekiStartHP,phase.raigeki);
        }
        return body;
    }

    static public ArrayList<String> HenseiRowHeader(){
        ArrayList<String> header = DayPhaseRowHeader();
        header.add("昼戦|夜戦");
        for(int i=1;i<=6;i++){
            String index = (new Integer(i)).toString();
            header.addAll(
                ShipRowHeader()
                    .stream()
                    .map(s->"自軍"+index+"."+s)
                    .collect(Collectors.toList())
            );
        }
        for(int i=1;i <= 6; ++i){
            String index = (new Integer(i)).toString();
            ShipSummaryRowHeader()
                .stream()
                .forEach(s->header.add("敵軍"+index+"."+s));
        }
        for(int i=1;i<=6;i++){
            String index = (new Integer(i)).toString();
            header.addAll(
                ShipRowHeader()
                    .stream()
                    .map(s->"自軍連合第二艦隊"+index+"."+s)
                    .collect(Collectors.toList())
            );
        }
        for(int i=1;i <= 6; ++i){
            String index = (new Integer(i)).toString();
            ShipSummaryRowHeader()
                .stream()
                .forEach(s->header.add("敵軍連合第二艦隊"+index+"."+s));
        }
        header.add("艦隊種類");
        header.add("敵艦隊種類");
        return header;
    }

    private ArrayList<ArrayList<String>>HenseiRowBody(Phase phase,LinkedTreeMap tree,ArrayList<ArrayList<String>> enemyRows,ArrayList<ArrayList<String>> friendRows,ArrayList<ArrayList<String>> enemyCombinedRows,ArrayList<ArrayList<String>> combinedRows,BuiltinScriptFilter filter){
        ArrayList<ArrayList<String>> body = new ArrayList<ArrayList<String>>();

        ArrayList<ArrayList<String>> enemySummaryRows = new ArrayList<ArrayList<String>>();
        for(int i=0;i<this.enemy.size();i++){enemySummaryRows.add(this.ShipSummaryRowBody(this.enemy.get(i)));}
        for(int i=this.enemy.size();i<6;i++){enemySummaryRows.add(this.ShipSummaryRowBody(null));}
        ArrayList<ArrayList<String>> enemyCombinedSummaryRows = new ArrayList<ArrayList<String>>();
        if(this.isEnemyCombined()){
            for(int i=0;i<this.enemyCombined.size();i++){enemyCombinedSummaryRows.add(this.ShipSummaryRowBody(this.enemyCombined.get(i)));}
            for(int i=this.enemyCombined.size();i<6;i++){enemyCombinedSummaryRows.add(this.ShipSummaryRowBody(null));}
        }
        else{
            for(int i=0;i<6;i++){enemyCombinedSummaryRows.add(this.ShipSummaryRowBody(null));}
        }
        if(friendRows == null){
            friendRows = new ArrayList<ArrayList<String>>();
            if(this.getDock()!=null){
                List<ShipDto> ships = this.getDock().getShips();
                for(int i=0;i<ships.size();i++){ friendRows.add(this.ShipRowBodyBase(ships.get(i), this.maxFriendHp[i], i));}
                for(int i=ships.size();i<6;i++){ friendRows.add(this.ShipRowBodyBase(null, 0, i));}
            }else{
                for(int i=0;i<6;i++){ friendRows.add(this.ShipRowBodyBase(null, 0, i));}
            }
        }
        if(combinedRows == null){
            combinedRows = new ArrayList<ArrayList<String>>();
            if(this.isCombined() && this.getDockCombined()!=null){
                List<ShipDto> ships = this.getDockCombined().getShips();
                for(int i=0;i<ships.size();i++){ combinedRows.add(this.ShipRowBodyBase(ships.get(i), this.maxFriendHpCombined[i], i+6));}
                for(int i=ships.size();i<6;i++){ combinedRows.add(this.ShipRowBodyBase(null, 0, i+6));}
            }else{
                for(int i=0;i<6;i++){ combinedRows.add(this.ShipRowBodyBase(null, 0, i+6));}
            }
        }
        int[][]phaseStartHP = new int[3][];
        if(phase == this.getPhase1()){
            phaseStartHP[0] = this.startEnemyHp.clone();
            phaseStartHP[1] = this.startFriendHp.clone();
            phaseStartHP[2] = (this.isCombined()) ?this.startFriendHpCombined.clone() :null;
        }
        else{
            Phase phase1 = this.getPhase1();
            phaseStartHP[0] = phase1.nowEnemyHp.clone();
            phaseStartHP[1] = phase1.nowFriendHp.clone();
            phaseStartHP[2] = (this.isCombined()) ?phase1.nowFriendHpCombined.clone() :null;
        }

        ArrayList<String> row = null;
        if(phase.isNight){
            row = this.NightPhaseRowBody();
            row.add("夜戦");
        }else{
            row = this.DayPhaseRowBody();
            row.add("昼戦");
        }
        for (int i = 0; i < 6; ++i) { row.addAll((i<this.maxFriendHp.length)?this.ShipRowBodyUpdate(friendRows.get(i),phaseStartHP[1][i],this.maxFriendHp[i]):friendRows.get(i)); }
        for (int i = 0; i < 6; ++i) { row.addAll(enemySummaryRows.get(i)); }
        for (int i = 0; i < 6; ++i) { row.addAll((this.isCombined() && i<this.maxFriendHpCombined.length)?this.ShipRowBodyUpdate(combinedRows.get(i),phaseStartHP[2][i],this.maxFriendHpCombined[i]):combinedRows.get(i)); }
        for (int i = 0; i < 6; ++i) { row.addAll(enemyCombinedSummaryRows.get(i)); }

        int combinedFlag = this.getCombinedKind();
        String combinedFlagString =
            (combinedFlag == 0)?"通常艦隊":
            (combinedFlag == 1)?"機動部隊":
            (combinedFlag == 2)?"水上部隊":
            (combinedFlag == 3)?"輸送部隊":
            "不明";
        row.add(combinedFlagString);
        row.add(isEnemyCombined()?"連合艦隊":"通常艦隊");
        if(filter.filterOutput(row)){
            body.add(row);
        }
        return body;
    }


    static public ArrayList<String> HenseiSakutekiRowHeader(){
        ArrayList<String> header = PhaseRowHeader();
        header.add("索敵スコア");
        header.add("分岐点係数");
        header.add("素索敵スコア合計");
        header.add("装備スコア合計");
        header.add("司令部スコア");
        header.add("人数スコア");
        for(int i=1;i<=6;i++){
            String index = (new Integer(i)).toString();
            header.addAll(
                ShipSakutekiRowHeader()
                    .stream()
                    .map(s->"自軍"+index+"."+s)
                    .collect(Collectors.toList())
            );
        }
        return header;
    }
    private ArrayList<ArrayList<String>>HenseiSakutekiRowBody(Phase phase,LinkedTreeMap tree,ArrayList<ArrayList<String>> enemyRows,ArrayList<ArrayList<String>> friendRows,ArrayList<ArrayList<String>> enemyCombinedRows,ArrayList<ArrayList<String>> combinedRows,BuiltinScriptFilter filter){
        ArrayList<ArrayList<String>> body = new ArrayList<ArrayList<String>>();
        ArrayList<ArrayList<String>> sakutekiFriendRows = new ArrayList<ArrayList<String>>();
        double withoutItemScore = 0.0;
        double itemScore = 0.0;
        double levelScore = -Math.ceil(0.4 * this.getHqLv());
        int countScore = 0;
        ArrayList<Integer> indexArray = new ArrayList<Integer>();
        if(this.getDock()!=null){
            List<ShipDto> ships = this.getDock().getShips();
            for(int i=0;i<ships.size();i++){ sakutekiFriendRows.add(this.ShipSakutekiRowBodyBase(ships.get(i), i));}
            for(int i=ships.size();i<6;i++){ sakutekiFriendRows.add(this.ShipSakutekiRowBodyBase(null, i));}
            for(ShipDto ship:ships){
                withoutItemScore += ship.getSakutekiScoreWithoutItem();
                itemScore += ship.getItemSakutekiScore();
            }
            countScore = 2 * (6 - ships.size());
            for(int i=0;i<ships.size();i++){
                indexArray.add(i);
            }
            indexArray.sort(Comparator.comparing(i -> ships.get((Integer)i).getShipId()));
        }else{
            for(int i=0;i<6;i++){ sakutekiFriendRows.add(this.ShipSakutekiRowBodyBase(null, i)); }
        }
        ArrayList<String> row = this.PhaseRowBody();
        double bunki = AppConfig.get().getBunkitenKeisu();
        double small = 0.00000000000001;
        double ten = 10000000000.0;
        row.add(String.format("%.10f",(Math.floor((withoutItemScore+itemScore*bunki+levelScore+countScore +small)*ten)+0.1)/ten));
        row.add(String.format("%.2f",(Math.floor((bunki+small)*100.0)+0.1)/100.0));
        row.add(String.format("%.10f",(Math.floor((withoutItemScore+small)*ten)+0.1)/ten));
        row.add(String.format("%.10f",(Math.floor((itemScore+small)*ten)+0.1)/ten));
        row.add(String.format("%.0f",levelScore));
        row.add(String.valueOf(countScore));
        for (int i=0;i<indexArray.size();i++){row.addAll(sakutekiFriendRows.get(indexArray.get(i)));}
        for (int i = indexArray.size(); i < 6; ++i) { row.addAll(sakutekiFriendRows.get(i)); }
        if(filter.filterOutput(row)){
            body.add(row);
        }
        return body;
    }

    /**砲撃戦夜戦とかは含まない*/
    public static ArrayList<String> BuiltinScriptKeys(){
        ArrayList<String> list = new ArrayList<String>();
        list.add("砲撃戦");
        list.add("夜戦");
        list.add("雷撃戦");
        list.add("航空戦");
        list.add("航空戦撃墜");
        list.add("基地航空戦");
        list.add("編成");
        list.add("編成索敵");
        return list;
    }
    /**砲撃戦夜戦とかは含まない*/
    public static Map<String,String[]> BuiltinScriptHeader(){
        try{
            HashMap<String,String[]>result = new HashMap<String,String[]>();
            BuiltinScriptKeys()
                .stream()
                .forEach(key->result.put(key,BuiltinScriptHeaderWithKey(key)));
            return result;
        }
        catch (Exception e) {
            return new HashMap<String,String[]>();
        }
    }
    private static HashMap<String,String[]> _headerCache = new HashMap<>();
    public static String[] BuiltinScriptHeaderWithKey(String key){
        try{
            if(_headerCache.get(key)!=null){ return _headerCache.get(key); }
            switch(key){
                case"砲撃戦":
                    _headerCache.put(key,HougekiRowHeader().toArray(new String[0]));
                    break;
                case"夜戦":
                    _headerCache.put(key,YasenRowHeader().toArray(new String[0]));
                    break;
                case"砲撃戦夜戦":
                    _headerCache.put(key,HougekiRowHeader().toArray(new String[0]));
                    break;
                case"雷撃戦":
                    _headerCache.put(key,RaigekiRowHeader().toArray(new String[0]));
                    break;
                case"航空戦":
                    _headerCache.put(key,AirRowHeader().toArray(new String[0]));
                    break;
                case"航空戦撃墜":
                    _headerCache.put(key, AirLostRowHeader().toArray(new String[0]));
                    break;
                case"基地航空戦":
                    _headerCache.put(key,BaseAirRowHeader().toArray(new String[0]));
                    break;
                case"編成":
                    _headerCache.put(key,HenseiRowHeader().toArray(new String[0]));
                    break;
                case"編成索敵":
                    _headerCache.put(key,HenseiSakutekiRowHeader().toArray(new String[0]));
                    break;
            }
            return _headerCache.get(key);
        }
        catch (Exception e) {
            return new String[0];
        }
    }


    /**
     * 初期化終わってるやつからBody取得
     * @return keyに対応する出力Body
     */
    public String[][] BuiltinScriptBodyWithKey(String key,LinkedTreeMap[] treeArray,String dateString,ArrayList<ArrayList<String>> enemyRows,ArrayList<ArrayList<String>> friendRows,ArrayList<ArrayList<String>> enemyCombinedRows,ArrayList<ArrayList<String>> combinedRows){
        return BuiltinScriptBodyWithKey(key,treeArray,dateString,enemyRows,friendRows,enemyCombinedRows,combinedRows,BuiltinScriptFilter.createTrueFilter());
    }
    public String[][] BuiltinScriptBodyWithKey(String key,LinkedTreeMap[] treeArray,String dateString,ArrayList<ArrayList<String>> enemyRows,ArrayList<ArrayList<String>> friendRows,ArrayList<ArrayList<String>> enemyCombinedRows,ArrayList<ArrayList<String>> combinedRows,BuiltinScriptFilter filter){
        try{
            if (this.exVersion >= 2) {
                if(dateString == null){
                    try{
                        FastDateFormat format = FastDateFormat.getInstance(AppConfig.get().getBuiltinDateFormat(),TimeZone.getTimeZone("JST"));
                        dateString = format.format(this.battleDate);
                    }catch(Exception ex){
                        dateString = DateTimeString.toString(this.battleDate);
                    }
                }
                if(treeArray == null){ treeArray = this.phaseList.stream().map(p->getGson().fromJson(p.json,LinkedTreeMap.class)).collect(Collectors.toList()).toArray(new LinkedTreeMap[0]); }
                switch(key){
                    case"砲撃戦":
                        try{
                            ArrayList<ArrayList<String>> body = this.HougekiRowBody(this.getPhase1(),treeArray[0],enemyRows,friendRows,enemyCombinedRows,combinedRows,filter);
                            String[][]array = new String[body.size()][];
                            for(int i=0;i<array.length;i++){
                                array[i] = body.get(i).toArray(new String[0]);
                                array[i][0] = dateString;
                            }
                            return array;
                        }catch (Exception e){
                            LOG.get().warn("砲撃戦出力に失敗しました", e);
                            String[]row = new String[this.HougekiRowHeader().size()];
                            for(int i=0;i<row.length;i++){
                                row[i] = "例外発生";
                            }
                            row[0] = dateString;
                            String[][]error = new String[1][];
                            error[0] = row;
                            return error;
                        }
                    case"夜戦":
                        try{
                            ArrayList<ArrayList<String>> body = new ArrayList<ArrayList<String>>();
                            if(this.getPhase1() != null && this.getPhase1().isNight){
                                body.addAll(this.YasenRowBody(this.getPhase1(),treeArray[0],enemyRows,friendRows,enemyCombinedRows,combinedRows,filter));
                            }
                            if(this.getPhase2() != null && this.getPhase2().isNight){
                                body.addAll(this.YasenRowBody(this.getPhase2(),treeArray[1],enemyRows,friendRows,enemyCombinedRows,combinedRows,filter));
                            }
                            String[][]array = new String[body.size()][];
                            for(int i=0;i<array.length;i++){
                                array[i] = body.get(i).toArray(new String[0]);
                                array[i][0] = dateString;
                            }
                            return array;
                        }catch (Exception e){
                            LOG.get().warn("夜戦出力に失敗しました", e);
                            String[]row = new String[this.YasenRowHeader().size()];
                            for(int i=0;i<row.length;i++){
                                row[i] = "例外発生";
                            }
                            row[0] = dateString;
                            String[][]error = new String[1][];
                            error[0] = row;
                            return error;
                        }
                    case"砲撃戦夜戦":
                        try{
                            ArrayList<ArrayList<String>> body = new ArrayList<ArrayList<String>>();
                            if(this.getPhase1() != null){
                                if(this.getPhase1().isNight){
                                    body.addAll(this.YasenRowBody(this.getPhase1(),treeArray[0],enemyRows,friendRows,enemyCombinedRows,combinedRows,filter));
                                }else{
                                    body.addAll(this.HougekiRowBody(this.getPhase1(),treeArray[0],enemyRows,friendRows,enemyCombinedRows,combinedRows,filter));
                                }
                            }
                            if(this.getPhase2() != null){
                                if(this.getPhase2().isNight){
                                    body.addAll(this.YasenRowBody(this.getPhase2(),treeArray[1],enemyRows,friendRows,enemyCombinedRows,combinedRows,filter));
                                }else{
                                    body.addAll(this.HougekiRowBody(this.getPhase2(),treeArray[1],enemyRows,friendRows,enemyCombinedRows,combinedRows,filter));
                                }
                            }
                            String[][]array = new String[body.size()][];
                            for(int i=0;i<array.length;i++){
                                array[i] = body.get(i).toArray(new String[0]);
                                array[i][0] = dateString;
                            }
                            return array;
                        }catch (Exception e){
                            LOG.get().warn("砲撃戦夜戦出力に失敗しました", e);
                            String[]row = new String[this.HougekiRowHeader().size()];
                            for(int i=0;i<row.length;i++){
                                row[i] = "例外発生";
                            }
                            row[0] = dateString;
                            String[][]error = new String[1][];
                            error[0] = row;
                            return error;
                        }
                    case"雷撃戦":
                        try{
                            ArrayList<ArrayList<String>> body = this.RaigekiRowBody(this.getPhase1(),treeArray[0],enemyRows,friendRows,enemyCombinedRows,combinedRows,filter);
                            String[][]array = new String[body.size()][];
                            for(int i=0;i<array.length;i++){
                                array[i] = body.get(i).toArray(new String[0]);
                                array[i][0] = dateString;
                            }
                            return array;
                        }catch (Exception e){
                            LOG.get().warn("雷撃戦出力に失敗しました", e);
                            String[]row = new String[this.RaigekiRowHeader().size()];
                            for(int i=0;i<row.length;i++){
                                row[i] = "例外発生";
                            }
                            row[0] = dateString;
                            String[][]error = new String[1][];
                            error[0] = row;
                            return error;
                        }
                    case"航空戦":
                        try{
                            ArrayList<ArrayList<String>> body = this.AirRowBody(this.getPhase1(),treeArray[0],enemyRows,friendRows,enemyCombinedRows,combinedRows,filter);
                            String[][]array = new String[body.size()][];
                            for(int i=0;i<array.length;i++){
                                array[i] = body.get(i).toArray(new String[0]);
                                array[i][0] = dateString;
                            }
                            return array;
                        }catch (Exception e){
                            LOG.get().warn("航空戦出力に失敗しました", e);
                            String[]row = new String[this.AirRowHeader().size()];
                            for(int i=0;i<row.length;i++){
                                row[i] = "例外発生";
                            }
                            row[0] = dateString;
                            String[][]error = new String[1][];
                            error[0] = row;
                            return error;
                        }
                    case"航空戦撃墜":
                        try{
                            ArrayList<ArrayList<String>> body = this.AirLostRowBody(this.getPhase1(),treeArray[0],enemyRows,friendRows,enemyCombinedRows,combinedRows,filter);
                            String[][]array = new String[body.size()][];
                            for(int i=0;i<array.length;i++){
                                array[i] = body.get(i).toArray(new String[0]);
                                array[i][0] = dateString;
                            }
                            return array;
                        }catch (Exception e){
                            LOG.get().warn("航空戦撃墜出力に失敗しました", e);
                            String[]row = new String[this.AirLostRowHeader().size()];
                            for(int i=0;i<row.length;i++){
                                row[i] = "例外発生";
                            }
                            row[0] = dateString;
                            String[][]error = new String[1][];
                            error[0] = row;
                            return error;
                        }
                    case"基地航空戦":
                        try{
                            ArrayList<ArrayList<String>> body = this.BaseAirRowBody(this.getPhase1(),treeArray[0],enemyRows,friendRows,enemyCombinedRows,combinedRows,filter);
                            String[][]array = new String[body.size()][];
                            for(int i=0;i<array.length;i++){
                                array[i] = body.get(i).toArray(new String[0]);
                                array[i][0] = dateString;
                            }
                            return array;
                        }catch (Exception e){
                            LOG.get().warn("基地航空戦出力に失敗しました", e);
                            String[]row = new String[this.BaseAirRowHeader().size()];
                            for(int i=0;i<row.length;i++){
                                row[i] = "例外発生";
                            }
                            row[0] = dateString;
                            String[][]error = new String[1][];
                            error[0] = row;
                            return error;
                        }
                    case"編成":
                        try{
                            ArrayList<ArrayList<String>> body = new ArrayList<ArrayList<String>>();
                            if(this.getPhase1() != null){
                                body.addAll(this.HenseiRowBody(this.getPhase1(),treeArray[0],enemyRows,friendRows,enemyCombinedRows,combinedRows,filter));
                            }
                            if(this.getPhase2() != null){
                                body.addAll(this.HenseiRowBody(this.getPhase2(),treeArray[1],enemyRows,friendRows,enemyCombinedRows,combinedRows,filter));
                            }
                            String[][]array = new String[body.size()][];
                            for(int i=0;i<array.length;i++){
                                array[i] = body.get(i).toArray(new String[0]);
                                array[i][0] = dateString;
                            }
                            return array;
                        }catch (Exception e){
                            LOG.get().warn("編成出力に失敗しました", e);
                            String[]row = new String[this.HenseiRowHeader().size()];
                            for(int i=0;i<row.length;i++){
                                row[i] = "例外発生";
                            }
                            row[0] = dateString;
                            String[][]error = new String[1][];
                            error[0] = row;
                            return error;
                        }
                    case"編成索敵":
                        try{
                            ArrayList<ArrayList<String>> body = new ArrayList<ArrayList<String>>();
                            if(this.getPhase1() != null){
                                body.addAll(this.HenseiSakutekiRowBody(this.getPhase1(),treeArray[0],enemyRows,friendRows,enemyCombinedRows,combinedRows,filter));
                            }
                            String[][]array = new String[body.size()][];
                            for(int i=0;i<array.length;i++){
                                array[i] = body.get(i).toArray(new String[0]);
                                array[i][0] = dateString;
                            }
                            return array;
                        }catch (Exception e){
                            LOG.get().warn("編成索敵出力に失敗しました", e);
                            e.printStackTrace();
                            String[]row = new String[this.HenseiSakutekiRowHeader().size()];
                            for(int i=0;i<row.length;i++){
                                row[i] = "例外発生";
                            }
                            row[0] = dateString;
                            String[][]error = new String[1][];
                            error[0] = row;
                            return error;
                        }
                    default:
                        return new String[0][];
                }
            }
            else{
                return new String[0][];
            }
        }
        catch (Exception e) {
            return new String[0][];
        }
    }

    public Map<String,String[][]> BuiltinScriptBody(){
        return this.BuiltinScriptBody(false);
    }
    /**
     * json読んで初期化と同時にスクリプト実行してBody取得
     * @return 各スクリプトの出力Body
     */
    public Map<String,String[][]> BuiltinScriptBody(boolean readJson){
        try{
            HashMap<String,String[][]>result = new HashMap<String,String[][]>();
            if (this.exVersion >= 2) {
                String _dateString;
                try{
                    FastDateFormat format = FastDateFormat.getInstance(AppConfig.get().getBuiltinDateFormat(),TimeZone.getTimeZone("JST"));
                    _dateString = format.format(this.battleDate);
                }catch(Exception ex){
                    _dateString = DateTimeString.toString(this.battleDate);
                }
                final String dateString = _dateString;
                LinkedTreeMap[] treeArray = new LinkedTreeMap[this.phaseList.size()];
                //初期化
                if(readJson){
                    Phase[] phaseCopy = this.phaseList.toArray(new Phase[0]);
                    this.enemy.clear();
                    this.phaseList.clear();
                    for (int i=0;i<phaseCopy.length;i++) {
                        Phase phase = phaseCopy[i];
                        String json = phase.json;
                        LinkedTreeMap tree = getGson().fromJson(json, LinkedTreeMap.class);
                        treeArray[i] = tree;
                        this.addPhase2(tree,json, phase.getKind());
                    }
                    this.readResultJson2(getGson().fromJson(this.resultJson,LinkedTreeMap.class));
                }else{
                    for (int i=0;i<this.phaseList.size();i++) {
                        Phase phase = this.phaseList.get(i);
                        String json = phase.json;
                        LinkedTreeMap tree = getGson().fromJson(json, LinkedTreeMap.class);
                        treeArray[i] = tree;
                    }
                }

                ArrayList<ArrayList<String>> enemyRows = new ArrayList<ArrayList<String>>();
                for(int i=0;i<this.enemy.size();i++){ enemyRows.add(this.ShipRowBodyBase(this.enemy.get(i), this.maxEnemyHp[i], i));}
                for(int i=this.enemy.size();i<6;i++){ enemyRows.add(this.ShipRowBodyBase(null,0, i));}
                ArrayList<ArrayList<String>> friendRows = new ArrayList<ArrayList<String>>();
                if(this.getDock()!=null){
                    List<ShipDto> ships = this.getDock().getShips();
                    for(int i=0;i<ships.size();i++){ friendRows.add(this.ShipRowBodyBase(ships.get(i), this.maxFriendHp[i], i));}
                    for(int i=ships.size();i<6;i++){ friendRows.add(this.ShipRowBodyBase(null, 0, i));}
                }else{
                    for(int i=0;i<6;i++){ friendRows.add(this.ShipRowBodyBase(null, 0, i));}
                }
                ArrayList<ArrayList<String>> combinedEnemyRows = new ArrayList<ArrayList<String>>();
                if(this.isEnemyCombined()){
                    for(int i=0;i<this.enemyCombined.size();i++){ combinedEnemyRows.add(this.ShipRowBodyBase(this.enemyCombined.get(i), this.maxEnemyHpCombined[i], i+6));}
                    for(int i=this.enemyCombined.size();i<6;i++){ combinedEnemyRows.add(this.ShipRowBodyBase(null,0, i+6));}
                }else{
                    for(int i=0;i<6;i++){ combinedEnemyRows.add(this.ShipRowBodyBase(null, 0, i+6));}
                }
                ArrayList<ArrayList<String>> combinedRows = new ArrayList<ArrayList<String>>();
                if(this.isCombined() && this.getDockCombined()!=null){
                    List<ShipDto> ships = this.getDockCombined().getShips();
                    for(int i=0;i<ships.size();i++){ combinedRows.add(this.ShipRowBodyBase(ships.get(i), this.maxFriendHpCombined[i], i+6));}
                    for(int i=ships.size();i<6;i++){ combinedRows.add(this.ShipRowBodyBase(null, 0, i+6));}
                }else{
                    for(int i=0;i<6;i++){ combinedRows.add(this.ShipRowBodyBase(null, 0, i+6));}
                }
                BuiltinScriptKeys()
                    .stream()
                    .forEach(key->result.put(key, BuiltinScriptBodyWithKey(key,treeArray,dateString,enemyRows,friendRows,combinedEnemyRows,combinedRows)));
            }
            return result;
        }
        catch (Exception e) {
            return new HashMap<String,String[][]>();
        }
    }

    public String[][] BodyWithFilter(BuiltinScriptFilter filter){
        if(filter.filterDateTime(this.getBattleDate())){
            return this.BuiltinScriptBodyWithKey(filter.key, null, null, null, null,null, null,filter);
        }else{
            return new String[0][];
        }
    }
    /**
     * 連合艦隊フラグ 連合の種類が特定できなかった場合は-1
     * 過去の記録には保存されていないはずなので編成から判断するが決定不能なケースもよくある
     * @return combinedFlag
     */
    private int calcCombinedFlag(){
        if(this.isCombined() == false){
            return 0;
        }

        if(this.phaseList.isEmpty()){
            return -1;
        }
        Phase phase = this.phaseList.get(0);
        if(phase.isNight()){
            return -1;
        }
        else if(phase.getApi().equals(DataType.COMBINED_BATTLE_WATER.getApiName())||phase.getApi().equals(DataType.COMBINED_EACH_BATTLE_WATER.getApiName())){
            return 2;
        }
        else if(phase.getApi().equals(DataType.COMBINED_BATTLE.getApiName())||phase.getApi().equals(DataType.COMBINED_EACH_BATTLE.getApiName())){
            //輸送と機動の区別は編成条件によるゴリ押し消去法しかない?
            //空き枠は轟沈考慮
            if(this.getDock() == null || this.getDockCombined() == null){
                return -1;
            }
            {
                int type1[];
                {
                    List<ShipDto> ships1 = this.getDock().getShips();
                    type1 =  new int[ships1.size()];
                    for(int i=0;i<type1.length;i++){
                        type1[i] = ships1.get(i).getStype();
                    }
                }
                int aki = 6-type1.length;
                {
                    int kuubo = 0;
                    int senkan = 0;
                    for(int type:type1){
                        switch(type){
                            case 7:
                            case 11:
                            case 18:
                                kuubo++;break;
                            case 8:
                            case 9:
                            case 10:
                                senkan++;break;
                        }
                    }
                    if(kuubo + aki < 2 || 4 < kuubo || 2 < senkan){
                        return 3;
                    }
                }
                {
                    int kuchiku = 0;
                    int keijunRenjun = 0;
                    int koujun = 0;
                    int kousen = 0;
                    int suibo = 0;
                    int youriku = 0;
                    int senbo = 0;
                    int hokyu = 0;
                    int sonota = 0;
                    for(int type:type1){
                        switch(type){
                            case 2:
                                kuchiku++;break;
                            case 3:
                            case 21:
                                keijunRenjun++;break;
                            case 6:
                                koujun++;break;
                            case 10:
                                kousen++;break;
                            case 16:
                                suibo++; break;
                            case 17:
                                youriku++;break;
                            case 20:
                                senbo++;break;
                            case 22:
                                hokyu++;break;
                            default:
                                sonota++;break;
                        }
                    }
                    if(kuchiku + aki < 4 || 2 < keijunRenjun || 2 < koujun || 2 < kousen || 2 < suibo || 1 < youriku || 1 < senbo || 1 < hokyu || 0 < sonota ){
                        return 1;
                    }
                }
            }
            {
                int type2[];
                {
                    List<ShipDto> ships2 = this.getDockCombined().getShips();
                    type2 =  new int[ships2.size()];
                    for(int i=0;i<type2.length;i++){
                        type2[i] = ships2.get(i).getStype();
                    }
                }
                int aki = 6-type2.length;
                {
                    int keijun = 0;
                    int kuchiku = 0;
                    int jujunKoujun = 0;
                    int keikuubo = 0;
                    int suibo = 0;
                    int kousokuSenkan = 0;
                    int teisokuSenkanKousenSeikikuubo = 0;
                    for(int type:type2){
                        switch(type){
                            case 3:
                                keijun++;break;
                            case 2:
                                kuchiku++;break;
                            case 5:
                            case 6:
                                jujunKoujun++;break;
                            case 7:
                                keikuubo++;break;
                            case 16:
                                suibo++;break;
                            case 8:
                                kousokuSenkan++;break;
                            case 9:
                            case 10:
                            case 11:
                                teisokuSenkanKousenSeikikuubo++;break;
                        }
                    }
                    if( keijun + aki < 1 || kuchiku + aki < 2 || 1 < keijun || 2 < jujunKoujun || 1 < keikuubo || 1 < suibo || 2 < kousokuSenkan || 0 < teisokuSenkanKousenSeikikuubo ){
                        return 3;
                    }
                }
                {
                    int keijunRenjun = 0;
                    int kuchiku = 0;
                    int jujunKoujun = 0;
                    int sonota = 0;
                    for(int type:type2){
                        switch(type){
                            case 3:
                            case 21:
                                keijunRenjun++;break;
                            case 2:
                                kuchiku++;break;
                            case 5:
                            case 6:
                                jujunKoujun++;break;
                            default:
                                sonota++;break;
                        }
                    }
                    if( keijunRenjun + aki < 1 || kuchiku + aki < 3 || 2 < keijunRenjun || 2 < jujunKoujun || 0 < sonota ){
                        return 1;
                    }
                }
            }
            return -1;
        }
        else{
            return -1;
        }
    }
    /**
     * 連合艦隊の種類を取得します
     * @return 連合艦隊の種類(0:未結成、1:機動部隊、2:水上部隊、3:輸送部隊、-x:強制解隊)
     */
    public int getCombinedKind() {
        if(this.isCombined()){
            if(this.combinedKind > 0){
                return this.combinedKind;
            }
            else{
                return this.calcCombinedFlag();
            }
        }else{
            return this.combinedKind;
        }
    }

    public void setCombinedKind(int combinedKind) {
        this.combinedKind = combinedKind;
    }
}
