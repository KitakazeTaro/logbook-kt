package logbook.builtinscript.akakariLog;

import logbook.data.AkakariData;
import logbook.data.Data;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import logbook.internal.LoggerHolder;
import org.apache.commons.lang3.time.FastDateFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by noratako5 on 2017/09/13.
 */

///出撃時のログを保存する。基地系ログ、羅針盤ログ、戦闘後ステータスログなど現状の戦闘単体の記録のみで不足する案件に使う。
public class AkakariSyutsugekiLogRecorder {
    private static LoggerHolder LOG = new LoggerHolder("builtinScript");
    //保存はmessagepack。
    //battlelog同様起動時に全て読み込み、戦闘日時と直後の艦情報のペアは持っておく。

    static private String syutsugekiLogPathOld = new File("akakariLog" + File.separator + "syutsugeki").getAbsolutePath();
    static public String syutsugekiLogPath = new File("akakariLog" + File.separator + "syutsugeki2").getAbsolutePath();
    static public String syutsugekiLogPathDateCache = new File("akakariLog" + File.separator + "syutsugekiDateCache").getAbsolutePath();
    static public String syutsugekiLogPathDateCache2 = new File("akakariLog" + File.separator + "syutsugekiDateCache2.dat").getAbsolutePath();
    static public  String syutsugekiLogPathRaw = new File("akakariLog" + File.separator + "syutsugekiRaw").getAbsolutePath();

    ///slotitemMember取得時点でリセット。
    ///糞重いので必要な装備だけ保存する。
    ///将来的には赤仮ログ単独でのcsv出力やるので保存しておくが、当分はbattlelog側の情報そのまま使うのでなくてもよい部分
    private static List<AkakariData> startSlotItemArray = new ArrayList<>();
    private static  AkakariData startPort = null;
    private static AkakariSyutsugekiLog log = null;

    public static void inputData(AkakariData data){
        try {
            if (log == null) {
                log = AkakariSyutsugekiLog.dataOrNull(startSlotItemArray, startPort);
            }
            switch (data.getDataType()) {
                case START2:
                    //これを捨てないとF5撤退を何度も繰り返した時にメモリが死ぬ
                    return;
                case PORT:
                    startPort = data;
                    break;
                case REQUIRE_INFO:
                case SLOTITEM_MEMBER:
                    startSlotItemArray = new ArrayList<>();
                    startSlotItemArray.add(data);
                    break;
                case CREATE_ITEM:
                case REMODEL_SLOT:
                case GET_SHIP:
                    startSlotItemArray.add(data);
                    break;
            }
            if (log != null) {
                log.inputData(data);
                if (log.isFinish()) {
                    if (log.needSave) {
                        saveData(log);
                    }
                    log = null;
                }
            }
        }
        catch (Exception e){
            LOG.get().warn(data.apiName,e);
        }
    }

    private static void saveData(AkakariSyutsugekiLog log){
        File dir = new File(syutsugekiLogPathRaw);
        if(!dir.exists()){
            if(!dir.mkdirs()){
                //作成失敗
                LOG.get().warn("保存失敗");
                return;
            }
        }
        try {
            {
                File dir2 = dateToDirPathRaw(log.start_port.date).toFile();
                if(!dir2.exists()){
                    if(!dir2.mkdirs()){
                        //作成失敗
                        LOG.get().warn("保存失敗");
                        return;
                    }
                }
            }
            File fileRaw = dateToPathRaw(log.start_port.date).toFile();
            File fileRawTmp = dateToPathRawTmp(log.start_port.date).toFile();
            File file = dateToPath(log.start_port.date).toFile();
            ArrayList<AkakariSyutsugekiLog> list = new ArrayList<>();
            if(fileRaw.exists() && fileRaw.length() > 0){
                AkakariSyutsugekiLog[] array = AkakariMapper.readSyutsugekiLogFromMessageRawFile(fileRaw);
                if(array == null){
                    LOG.get().warn("ロード失敗");
                    File file2Raw = dateToNextErrorPathRaw(log.start_port.date).toFile();
                    fileRaw.renameTo(file2Raw);
                }
                else {
                    list.addAll(Arrays.asList(array));
                }
            }
            else if(file.exists() && file.length() > 0){
                AkakariSyutsugekiLog[] array = AkakariMapper.readSyutsugekiLogFromMessageZstdFile(file);
                if(array == null){
                    LOG.get().warn("ロード失敗");
                    File file2 = dateToNextErrorPath(log.start_port.date).toFile();
                    file.renameTo(file2);
                }
                else {
                    list.addAll(Arrays.asList(array));
                }
            }
            list.add(log);
            AkakariSyutsugekiLog[] result = list.toArray(new AkakariSyutsugekiLog[0]);
            AkakariSyutsugekiLogReader.updateLogFile(fileRaw.toPath(),result);
            AkakariMapper.writeObjectToMessageRawFile(result,fileRawTmp);
            AkakariSyutsugekiLog[] result2 = AkakariMapper.readSyutsugekiLogFromMessageRawFile(fileRawTmp);
            if(result.length == result2.length){
                while(fileRaw.exists()) {
                    if(!fileRaw.delete()){
                        Thread.sleep(100);
                    }
                }
                if(fileRawTmp.renameTo(fileRaw)) {
                    fileRawTmp.delete();
                }
            }
            AkakariSyutsugekiLogReader.loadStartPortDate(log);
            //AkakariMapper.writeObjectToJsonFile(result,new File(syutsugekiLogPath+File.separator+"syutsugeki"+date+".json"));
        }
        catch (Exception e){
            LOG.get().warn("保存失敗",e);
        }
    }
    public static void createJson(File file){
        try {
            ArrayList<AkakariSyutsugekiLog> list = new ArrayList<>();
            if(file.exists() && file.length() > 0){
                AkakariSyutsugekiLog[] array = AkakariMapper.readSyutsugekiLogFromMessageZstdFile(file);
                if(array != null){
                    list.addAll(Arrays.asList(array));
                }
            }
            AkakariSyutsugekiLog[] result = list.toArray(new AkakariSyutsugekiLog[0]);
            AkakariMapper.writeObjectToJsonFile(result,new File(file.getAbsolutePath()+".json"));
        }
        catch (Exception e){
            LOG.get().warn("保存失敗",e);
        }
    }
    @NotNull
    public static Path dateToPath(Date startPortDate){
        FastDateFormat format =  FastDateFormat.getInstance("yyyy-MM-dd_HH", TimeZone.getTimeZone("JST"));
        String dateHour = format.format(startPortDate);
        File file = new File(dateToDirPath(startPortDate).toString()+File.separator+"syutsugeki"+dateHour+".dat");
        return file.toPath();
    }
    @NotNull
    public static Path dateToPathRaw(Date startPortDate){
        FastDateFormat format =  FastDateFormat.getInstance("yyyy-MM-dd_HH", TimeZone.getTimeZone("JST"));
        String dateHour = format.format(startPortDate);
        File file = new File(dateToDirPathRaw(startPortDate).toString()+File.separator+"syutsugeki"+dateHour+".dat");
        return file.toPath();
    }
    @NotNull
    public static Path dateToPathRawTmp(Date startPortDate){
        FastDateFormat format =  FastDateFormat.getInstance("yyyy-MM-dd_HH", TimeZone.getTimeZone("JST"));
        String dateHour = format.format(startPortDate);
        File file = new File(dateToDirPathRaw(startPortDate).toString()+File.separator+"syutsugeki_tmp_"+dateHour+".dat");
        return file.toPath();
    }
    ///現在時刻ベースで被らないパス
    @NotNull
    public static Path dateToNextErrorPath(Date startPortDate){
        FastDateFormat format =  FastDateFormat.getInstance("yyyy-MM-dd_HH", TimeZone.getTimeZone("JST"));
        String dateHour = format.format(startPortDate);
        FastDateFormat timeFormat =  FastDateFormat.getInstance("HH-mm-ss-SSS", TimeZone.getTimeZone("JST"));
        String time = timeFormat.format(startPortDate);
        File file = new File(dateToDirPath(startPortDate).toString()+File.separator+"syutsugeki"+dateHour+"_error_"+ time +".dat");
        return file.toPath();
    }
    ///現在時刻ベースで被らないパス
    @NotNull
    public static Path dateToNextErrorPathRaw(Date startPortDate){
        FastDateFormat format =  FastDateFormat.getInstance("yyyy-MM-dd_HH", TimeZone.getTimeZone("JST"));
        String dateHour = format.format(startPortDate);
        FastDateFormat timeFormat =  FastDateFormat.getInstance("HH-mm-ss-SSS", TimeZone.getTimeZone("JST"));
        String time = timeFormat.format(startPortDate);
        File file = new File(dateToDirPathRaw(startPortDate).toString()+File.separator+"syutsugeki"+dateHour+"_error_"+ time +".dat");
        return file.toPath();
    }
    @NotNull
    public static Path dateToDirPath(Date startPortDate){
        FastDateFormat formatDate =  FastDateFormat.getInstance("yyyy-MM-dd", TimeZone.getTimeZone("JST"));
        String date = formatDate.format(startPortDate);
        File file = new File(syutsugekiLogPath+File.separator+date);
        return file.toPath();
    }
    @NotNull
    public static Path dateToDirPathRaw(Date startPortDate){
        FastDateFormat formatDate =  FastDateFormat.getInstance("yyyy-MM-dd", TimeZone.getTimeZone("JST"));
        String date = formatDate.format(startPortDate);
        File file = new File(syutsugekiLogPathRaw+File.separator+date);
        return file.toPath();
    }
    public static Path dateToPathOld(Date startPortDate){
        FastDateFormat format =  FastDateFormat.getInstance("yyyy-MM-dd", TimeZone.getTimeZone("JST"));
        String date = format.format(startPortDate);
        File file = new File(syutsugekiLogPathOld+File.separator+"syutsugeki"+date+".dat");
        return file.toPath();
    }
    public static Path zstdToRawPath(Path zstd){
        String filename = zstd.getFileName().toString();
        String dirName = zstd.getParent().getFileName().toString();
        File file = new File(syutsugekiLogPathRaw+File.separator+dirName+File.separator+filename);
        return  file.toPath();
    }
    public static Path rawToZstdPath(Path raw){
        String filename = raw.getFileName().toString();
        String dirName = raw.getParent().getFileName().toString();
        File file = new File(syutsugekiLogPath+File.separator+dirName+File.separator+filename);
        return  file.toPath();
    }
    public static Path zstdToCachePath(Path zstd){
        String filename = zstd.getFileName().toString();
        String dirName = zstd.getParent().getFileName().toString();
        File file = new File(syutsugekiLogPathDateCache+File.separator+dirName+File.separator+filename);
        return  file.toPath();
    }
    @Nullable
    public static List<Path> allFilePath(){
        Path path = (new File(syutsugekiLogPath)).toPath();
        if(Files.exists(path)) {
            try {
                List<Path> allDir = Files.list(path).collect(Collectors.toList());
                ArrayList<Path> allPath = new ArrayList<>();
                for(Path p : allDir){
                    allPath.addAll(Files.list(p).collect(Collectors.toList()));
                }
                allPath.removeIf(p -> p.getFileName().toString().contains("error"));
                return allPath;
            } catch (Exception e) {
                LOG.get().warn("読み込み失敗", e);
                return null;
            }
        }
        else {
            return null;
        }
    }
    ///rawファイルが存在しないzstdのみ対象
    @Nullable
    public static List<Path> allFilePathWithoutRaw(){
        Path path = (new File(syutsugekiLogPath)).toPath();
        if(Files.exists(path)) {
            List<Path> allPath = allFilePath();
            if(allPath != null) {
                allPath.removeIf(p -> AkakariSyutsugekiLogRecorder.zstdToRawPath(p).toFile().exists());
            }
            return allPath;
        }
        else {
            return null;
        }
    }
    @Nullable
    public static List<Path> allFilePathRaw(){
        Path path = (new File(syutsugekiLogPathRaw)).toPath();
        if(Files.exists(path)) {
            try {
                List<Path> allDir = Files.list(path).collect(Collectors.toList());
                ArrayList<Path> allPath = new ArrayList<>();
                for(Path p : allDir){
                    allPath.addAll(Files.list(p).collect(Collectors.toList()));
                }
                allPath.removeIf(p -> p.getFileName().toString().contains("error"));
                return allPath;
            } catch (Exception e) {
                LOG.get().warn("読み込み失敗", e);
                return null;
            }
        }
        else {
            return null;
        }
    }

    @Nullable
    public static List<Path> allFilePathOld(){
        Path path = (new File(syutsugekiLogPathOld)).toPath();
        if(Files.exists(path)) {
            try {
                return Files.list(path).collect(Collectors.toList());
            } catch (Exception e) {
                LOG.get().warn("読み込み失敗", e);
                return null;
            }
        }
        else {
            return null;
        }
    }
}
