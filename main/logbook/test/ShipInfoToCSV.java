/**
 * 
 */
package logbook.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import logbook.config.ShipConfig;
import logbook.dto.ShipInfoDto;
import logbook.internal.Ship;

import org.apache.commons.lang3.StringUtils;

/**
 * @author iedeg_000
 *
 */
public class ShipInfoToCSV {

    /**
     * @param args
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        ShipConfig.load();
        FileWriter fw = new FileWriter(new File("shipInfo.csv"));

        fw.write(StringUtils.join(new String[] {
                "名前", "艦ID", "タイプID", "タイプ名", "改造Lv", "改造後の艦ID", "Flagship", "Max弾", "Max燃料" }, ','));
        fw.write("\n");

        for (String key : Ship.keySet()) {
            ShipInfoDto dto = Ship.get(key);
            if (dto.getName().length() > 0) {
                fw.write(StringUtils.join(new String[] {
                        dto.getName(),
                        Integer.toString(dto.getShipId()),
                        Integer.toString(dto.getStype()),
                        dto.getType(),
                        Integer.toString(dto.getAfterlv()),
                        Integer.toString(dto.getAftershipid()),
                        dto.getFlagship(),
                        Integer.toString(dto.getMaxBull()),
                        Integer.toString(dto.getMaxFuel()) }, ','));
                fw.write("\n");
            }
        }

        fw.close();
    }

}
