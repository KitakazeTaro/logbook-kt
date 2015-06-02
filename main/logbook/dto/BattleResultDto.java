package logbook.dto;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.json.JsonObject;

import logbook.constants.AppConstants;
import logbook.internal.UseItem;

import org.apache.commons.lang3.StringUtils;

/**
 * 海戦とドロップした艦娘を表します
 */
public class BattleResultDto extends AbstractDto {

    /** 日付 */
    private final Date battleDate;

    /** 海域名 */
    private final String questName;

    /** ランク */
    private final ResultRank rank;

    /** マス */
    private final MapCellDto mapCell;

    /** 敵艦隊名 */
    private final String enemyName;

    /** ドロップフラグ */
    private final boolean dropShip;

    /** ドロップフラグ */
    private final boolean dropItem;

    /** 艦種 */
    private final String dropType;

    /** 艦名 */
    private final String dropName;

    /** 母港の空きがない？ */
    private final boolean noSpaceForShip;

    /** スクリプトサポート */
    private final Comparable[] extData;

    /** 戦闘スクリプトサポート */
    private final Map<String, Comparable[][]> allCombatExtData;

    /**
     * コンストラクター
     * もう使われていない
     * @param object JSON Object
     * @param mapCellNo マップ上のマス
     * @param mapBossCellNo　ボスマス
     * @param eventId EventId
     * @param isStart 出撃
     * @param battle 戦闘
     */
    public BattleResultDto(JsonObject object, MapCellDto mapCell, BattleDto battle) {

        this.battleDate = Calendar.getInstance().getTime();
        this.questName = object.getString("api_quest_name");
        this.rank = ResultRank.fromRank(object.getString("api_win_rank"));
        this.mapCell = mapCell;
        this.enemyName = object.getJsonObject("api_enemy_info").getString("api_deck_name");
        this.dropShip = object.containsKey("api_get_ship");
        this.dropItem = object.containsKey("api_get_useitem");
        if (this.dropShip || this.dropItem) {
            if (this.dropShip) {
                this.dropType = object.getJsonObject("api_get_ship").getString("api_ship_type");
                this.dropName = object.getJsonObject("api_get_ship").getString("api_ship_name");
            } else {
                String name = UseItem.get(object.getJsonObject("api_get_ship").getInt("api_useitem_id"));
                this.dropType = "アイテム";
                this.dropName = StringUtils.defaultString(name);
            }
        } else {
            this.dropType = "";
            this.dropName = "";
        }

        this.noSpaceForShip = false;
        this.extData = null;
        this.allCombatExtData = null;
    }

    public BattleResultDto(BattleExDto dto, Comparable[] extData, Map<String, Comparable[][]> allCombatExtData) {
        this.battleDate = dto.getBattleDate();
        this.questName = dto.getQuestName();
        this.rank = dto.getRank();
        this.mapCell = dto.getMapCellDto();
        this.enemyName = dto.getEnemyName();
        this.dropShip = dto.isDropShip();
        this.dropItem = dto.isDropItem();
        this.dropType = dto.getDropType();
        this.dropName = dto.getDropName();
        this.noSpaceForShip = (dto.getExVersion() >= 1) && (dto.getShipSpace() == 0);
        this.extData = extData;
        this.allCombatExtData = allCombatExtData;
    }

    private boolean hasTaihaInFleet(int[] nowhp, int[] maxhp) {
        if ((nowhp == null) || (maxhp == null)) {
            return false;
        }
        for (int i = 0; i < nowhp.length; ++i) {
            double rate = (double) nowhp[i] / (double) maxhp[i];
            if (rate <= AppConstants.BADLY_DAMAGE) {
                return true;
            }
        }
        return false;
    }

    /**
     * 日付を取得します。
     * @return 日付
     */
    public Date getBattleDate() {
        return this.battleDate;
    }

    /**
     * 海域名を取得します。
     * @return 海域名
     */
    public String getQuestName() {
        return this.questName;
    }

    public boolean isPractice() {
        return (this.questName == null);
    }

    /**
     * ランクを取得します。
     * @return ランク
     */
    public ResultRank getRank() {
        return this.rank;
    }

    /**
     * マスを取得します。
     * @return マス
     */
    public MapCellDto getMapCell() {
        return this.mapCell;
    }

    /**
     * 出撃を取得します
     * @return 出撃
     */
    public boolean isStart() {
        return (this.mapCell != null) ? this.mapCell.isStart() : false;
    }

    /**
     * ボスマスを取得します
     * @return ボスマス
     */
    public boolean isBoss() {
        return (this.mapCell != null) ? this.mapCell.isBoss() : false;
    }

    /**
     * 出撃・ボステキストを取得します
     * @return 出撃・ボステキスト
     */
    public String getBossText() {
        if (this.isStart() || this.isBoss()) {
            List<String> list = new ArrayList<>();
            if (this.isStart()) {
                list.add("出撃");
            }
            if (this.isBoss()) {
                list.add("ボス");
            }
            return StringUtils.join(list, "&");
        }
        return "";
    }

    /**
     * 敵艦隊名を取得します。
     * @return 敵艦隊名
     */
    public String getEnemyName() {
        return this.enemyName;
    }

    /**
     * ドロップフラグを取得します。
     * @return ドロップフラグ
     */
    public boolean isDropShip() {
        return this.dropShip;
    }

    /**
     * ドロップフラグを取得します。
     * @return ドロップフラグ
     */
    public boolean isDropItem() {
        return this.dropItem;
    }

    /**
     * 艦種を取得します。
     * @return 艦種
     */
    public String getDropType() {
        return this.dropType;
    }

    /**
     * 艦名を取得します。
     * @return 艦名
     */
    public String getDropName() {
        return this.dropName;
    }

    /**
     * 表示するドロップ艦名
     * @return 艦名
     */
    public String getScreenDropName() {
        if (StringUtils.isEmpty(this.dropName) && this.noSpaceForShip) {
            return "※空きなし";
        }
        return this.dropName;
    }

    /**
     * @return noSpaceForShip
     */
    public boolean isNoSpaceForShip() {
        return this.noSpaceForShip;
    }

    /**
     * @return extData
     */
    public Comparable[] getExtData() {
        return this.extData;
    }

    /**
     * @return combatExtData
     */
    public Comparable[][] getCombatExtData(String name) {
        return this.allCombatExtData.get(name);
    }
}