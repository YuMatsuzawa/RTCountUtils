package matz;

/* RTログの中でRT数が上位XXX位のTweetリストを作る
 * とりあえずファイル開く＞行数カウント＞ループ＞ソート＞リスト化
 * その過程でデータの累積頻度分布を作る
 * 分布のドメインは4ヶ月で、1日＝1440分を1区間として分布化する
 */

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RTPicker {
	private static int pick = 1000; //ピックする数
	private static String infroot = "./";
	
	public RTPicker(String[] args) {
		/* コンストラクタはmainの冒頭で呼ばれ、コマンドライン引数argsをまるごと受けるが、実際には最初の2要素までのみを使う。
		 * 1要素しかない場合はそれが抽出数(int)かパス(String)かをparseの可否で判定する。 
		 * 0要素ならどちらの変数もデフォルト値を使う。
		 */
		if (args.length >= 2) {
			try {
				pick = Integer.parseInt(args[0]);
				infroot = args[1];
			} catch (NumberFormatException e) {
				pick = Integer.parseInt(args[1]);
				infroot = args[0];
			}
		} else if (args.length == 1) {
			try{
				pick = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				infroot = args[0];
			}
		}
	}

	private static HashMap<File, Integer> retweetCount = new HashMap<File, Integer>(); //ファイルごとRT数を数えるマップ
	//private static int[] accumFreq = new int[122];
	private static int fourMonths = 122;
	private static int minuteaDay = 1440; //1日1440分 
	private static int secondsaDay = 1440*60;

	private static int RTcountMin = Integer.MAX_VALUE;
	private static int RTcountMax = 0;
	
	public static int[] accum(File file, int[] accumFreq) throws Exception { //1日刻みの累積頻度分布(4ヶ月＝122日まで)を作る
		InputStreamReader isr = new InputStreamReader(new FileInputStream(file));
		BufferedReader br = new BufferedReader(isr);
		String line = br.readLine(); //先頭行(URL)
		
		int count = 0;
		while((line = br.readLine()) != null) {
			int lineInt = Integer.parseInt(line);
			for(int d=0; d<fourMonths; d++){
				if(lineInt < (d+1)*minuteaDay) accumFreq[d]++;
				else continue;
			}
			count++;
		}
		
		br.close();
		isr.close();
		
		retweetCount.put(file, count);
		if(count < RTcountMin) RTcountMin = count;
		if(count > RTcountMax) RTcountMax = count;
		
		return accumFreq;
	}

	public static void accumWrite(int[] accumFreq) throws Exception {
		File outf = new File("RTAccumFreq.csv");
		OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(outf));
		BufferedWriter bw = new BufferedWriter(osw);
		
		for(int i=0; i<fourMonths; i++){
			bw.write(i*minuteaDay + ", " + accumFreq[i]);
			bw.newLine();
		}
		
		bw.close();
		osw.close();
	}

	
	private static URL checkRedirect(URL queryURL) throws Exception { //リダイレクトが終わるまで再帰的にリダイレクト先に接続を開く
		HttpURLConnection queryConn = (HttpURLConnection) queryURL.openConnection();
		
		int queryStatus = queryConn.getResponseCode();
		if (queryStatus != HttpURLConnection.HTTP_OK) {
			if (queryStatus == HttpURLConnection.HTTP_MOVED_PERM 
					|| queryStatus == HttpURLConnection.HTTP_MOVED_TEMP
					|| queryStatus == HttpURLConnection.HTTP_SEE_OTHER) {
				URL redirectURL = new URL(queryConn.getHeaderField("Location"));
				return checkRedirect(redirectURL);
			}
		}
		return queryURL;
	}

	public static long getDate(String key) throws Exception {
		/* Webからオリジナルツイートの日時を取得してくる
		 * URLクラスと、そこからストリームを開くメソッドを使用
		 */
		URL originalURL = new URL(key);
		URL queryURL = checkRedirect(originalURL);
		
		InputStream is = queryURL.openStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String sourceLine;
		long ret = 0;
		while((sourceLine = br.readLine()) != null) {
			Pattern dataTime = Pattern.compile("(data-time=\")(\\d+)(\")");
			Matcher matcher = dataTime.matcher(sourceLine);
			if(matcher.find()){
				ret = Long.parseLong(matcher.group(2));
				break;
			}
		}
		br.close();
		is.close();
		if (ret != 0) return ret;
		else throw new DateNotFoundException();
	}
	
	@SuppressWarnings({ "deprecation", "unused" })
	private static Date middleOfPeriod = new Date(111, 8, 7, 23, 59, 59);
	private static int minimumRetweetCount = 1000;
	private static int halfOfPeriod = 60*1440*7;
	
	private static void RTcounter(File file) throws Exception { //数えるだけ
		InputStreamReader isr = new InputStreamReader(new FileInputStream(file));
		BufferedReader br = new BufferedReader(isr);
		String line = br.readLine(); //先頭行(URL)
		@SuppressWarnings("unused")
		String key = line;
		
		int count = 0;
		int dataMin = Integer.MAX_VALUE;
		int dataMax = 0;
		while((line = br.readLine()) != null) {
			count++;
			StringTokenizer st = new StringTokenizer(line, ",");
			int dataInt = Integer.parseInt(st.nextToken());
			if(dataInt < dataMin) dataMin = dataInt;
			if(dataInt > dataMax) dataMax = dataInt;
		}
		
		br.close();
		isr.close();
		
		/*long originalDate = getDate(key);
		int isFirstHalf = middleOfPeriod.compareTo(new Date(originalDate));*/
		
		if(dataMin < secondsaDay && dataMax > halfOfPeriod) retweetCount.put(file, count);
		if(count < RTcountMin) RTcountMin = count;
		if(count > RTcountMax) RTcountMax = count;
	}

	private static void RTRank() throws Exception {
		ArrayList<Map.Entry<File, Integer>> RTList = new ArrayList<Map.Entry<File, Integer>>(retweetCount.entrySet()); //Mapの内容をValueでソートしてListに移し替え
		Collections.sort(RTList, new Comparator<Map.Entry<File, Integer>>(){
			public int compare(Map.Entry<File, Integer> o1, Map.Entry<File, Integer> o2){
				Map.Entry<File, Integer> e1 = (Entry<File, Integer>)o1;
				Map.Entry<File, Integer> e2 = (Entry<File, Integer>)o2;
				return ((Integer)e1.getValue()).compareTo((Integer)e2.getValue());
			}
		});
		
		for(int i=1; i<=pick; i++){
				try {
				File file = RTList.get(RTList.size() - i).getKey();
				int rtcnt = RTList.get(RTList.size() - i).getValue();
				
				if (rtcnt >= minimumRetweetCount) {
					File fileCopyTo = new File("retweetLog" + pick, file.getName());
					
					if(!fileCopyTo.getParentFile().isDirectory()) fileCopyTo.getParentFile().mkdir();
		
					FileInputStream fis = new FileInputStream(file);
					FileOutputStream fisCopyTo = new FileOutputStream(fileCopyTo);
					FileChannel src = fis.getChannel();
					FileChannel dest = fisCopyTo.getChannel();
					
					src.transferTo(0, src.size(), dest);
					
					src.close();
					dest.close();
					
					fis.close();
					fisCopyTo.close();
					
					System.out.println(file.toString() + "\t" + rtcnt);
				} else {
					continue;
				}
			} catch (Exception e) {
				break;
			}
		}
	}
	
	public static void main (String[] args) {
		new RTPicker(args);
		
		try{
			File infpath = new File(infroot); //infrootにはデータのあるディレクトリそのものを指定する
			int[] accumFreq = new int[fourMonths];
			for (int i=0; i<fourMonths; i++) accumFreq[i] = 0;
			for (File file : infpath.listFiles()) {
				if (file.getName().matches("[\\d]+.csv")){ //データは数字列.csv形式
					try {
						//accumFreq = accum(file, accumFreq);	//ファイルごとRTを数えつつ累積頻度分布を作る
						RTcounter(file);						//数えるだけ
					} catch (FileNotFoundException e) {
						continue;
					}
				} else continue;
			}
			//全ファイル処理終了後
			
			//累積頻度分布出力
			//accumWrite(accumFreq);
			
			//RTランク付け&リスト作り
			RTRank();
		
		
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}



