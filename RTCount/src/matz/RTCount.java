package matz;

/* 公式RTをretweet_urlに値が何度入っていたかで数え上げる。
 * だいたいこのクラスはdssXXディレクトリ一つに対し8-9時間かかる。
 * 	$ java -jar RTCount.jar
 * でメインクラスが勝手に走る。追加するRTFreqを使うときは
 * 	$ java -cp RTCount.jar matz.RTFreq
 * でクラス指定して実行する。バックグラウンドプロセスにして、かつログアウト後も実行させるにはnohupを使う。
 * 	$ nohup java <option> <jarfile> [<class>[ <args>]] > RTCount.log 2> err.log &
 * 末尾の&がバックグラウンド指定子。argsにはパスあるいはログ閾値（または両方）を受けられる。
 * バグがあってnohupで開始した処理を殺したくなったら、kill -kill <pid>で止められる。pidは$ psか$ jpsで調べる。
 * dss11-14まで全て処理する。ただ、存在しないor空なディレクトリはスキップする。
 * RTCountもRTFreqも特別な外部パッケージを使っていないのでネイティブjava環境があるサーバならそのまま動くはず。
 * build.jardescがantではないがビルドファイルなのでこれをダブクリして次へ連打で設定不要。
 */

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RTCount {
	private static long logThreshold = 100; 
	private static String infroot = "decoded/hotlink/"; 
	public RTCount(String[] args) { 
		/* コンストラクタはmainの冒頭で呼ばれ、コマンドライン引数argsをまるごと受けるが、実際には最初の2要素までのみを使う。
		 * 1要素しかない場合はそれがログ閾値(long)かパス(String)かを判定する。 
		 */
		if (args.length >= 2) {
			try {
				logThreshold = Long.parseLong(args[0]);
				infroot = args[1];
			} catch (NumberFormatException e) {
				logThreshold = Long.parseLong(args[1]);
				infroot = args[0];
			}
		} else if (args.length == 1) {
			try{
				logThreshold = Long.parseLong(args[0]);
			} catch (NumberFormatException e) {
				infroot = args[0];
			}
		}
	}
	
	//実行時間計測
	private static long start = System.currentTimeMillis();
	private static long split = start;
	private static long resultWriteTimeSum = 0;
	private static long checkedNum = 0;
	private static long retweetedNum = 0;
	private static void stopWatch(String comment) {
		long current = System.currentTimeMillis();
		if (checkedNum > 0 && retweetedNum > 0) {
			resultWriteTimeSum += (current - split);
			double resultWriteAverageTime = resultWriteTimeSum / checkedNum;
			double remainingSec = resultWriteAverageTime * (retweetedNum - checkedNum) / 1000;
			double remainingMin = remainingSec / 60;
			comment = String.format("%s(%.1fmin remains...)", comment, remainingMin);
		}
		System.out.println((current - start) + "\t" + (current - split) + "\t" + comment);
		split = current;
	}
	
	/* key：オリジナルtweetのURL<String>,value：retweetされた時間とURLの配列を全て持つArrayList
	 * このマップは処理をすすめるごとにクソでかくなっていく((keyの数=retweetされたことのあるURLの数)*(valueのsize=retweetされた回数))
	 * 適切なタイミングでclear()を呼んでメモリを開放すること。
	 */
	private static HashMap<String, ArrayList<String[]>> retweeted = new HashMap<String, ArrayList<String[]>>();
	private static void resultWriter() throws IOException, ParseException { //retweetを選別してログ出力する
		File outfpath = new File("retweetLog" + start);
		if (!outfpath.isDirectory()) outfpath.mkdir();
		
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		retweetedNum = retweeted.size();
		checkedNum = 0;
		resultWriteTimeSum = 0;
		for(Entry<String, ArrayList<String[]>> set : retweeted.entrySet()) {
			
			String fname = set.getKey().replaceAll("http://(([^/]+)/)+", ""); //ログファイル名
			File outf = new File(outfpath, fname + ".csv");
			
			/* ログに残す条件は
			 * (1)1日辺りXXXリツイート以上
			 * (2)既にそのリツイートのログが存在 i.e.(1)を過去に1回以上満たした記録が存在
			 * のいずれか 
			 */
			checkedNum++;
			
			if (outf.exists() || set.getValue().size() >= logThreshold) { 
				
				Date originalDate;
				try {
					originalDate = new Date(1000 * getDate(set.getKey())); //webソースで見つかるUnix時間は秒なので1000倍してミリ秒にしておく
				} catch (Exception e) { //webソース周りで例外が投げられてもAbortして次に行く。タイムアウトとかdata-timeが見つからないとか。
					stopWatch("Aborting: " + set.getKey());
					continue;
				}
				
				OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(outf, true)); //append=trueにしておけば追記				
				BufferedWriter bw = new BufferedWriter(osw);


				if (outf.length() == 0) {
					stopWatch("Writing: " + outf.toString());
					bw.write(set.getKey()); //最初だけ元URLを書く。
					bw.newLine();
				} else stopWatch("Appending: " + outf.toString());
								
				for(String[] rawDateAndURL : set.getValue()) {
					Date retweetDate = df.parse(rawDateAndURL[0]);
					long expiredSec = (retweetDate.getTime() - originalDate.getTime()) / 1000;
					//String expiredMin = String.format("%d", expiredSec/60);
					bw.write(String.format("%d, %s", expiredSec, rawDateAndURL[1])); //データを分に直さずに取得する．
					bw.newLine();
				}
				bw.close();
				osw.close();
				
				stopWatch("Complete: " + outf.toString());
			}
		}
		resultWriteTimeSum = 0;
		checkedNum = 0;
		retweetedNum = 0;
	}
	
	public static String[] getValues (String line) { 
		Pattern retweetURL = Pattern.compile("(\t)(retweeted_url=\")(\\S+)(\"\t)"); 
		Matcher matcher1 = retweetURL.matcher(line);
		String[] ret = {null, null, null};
		while (matcher1.find()) {
			ret[0] = matcher1.group(3);
		}
		
		Pattern date = Pattern.compile("(\t)(date=\")([^\t]+)(\"\t)");
		matcher1 = date.matcher(line);
		while (matcher1.find()) {
			ret[1] = matcher1.group(3);
		}
		
		Pattern tweetURL = Pattern.compile("(\t)(url=\")(\\S+)(\"\t)");
		matcher1 = tweetURL.matcher(line);
		while (matcher1.find()) {
			ret[2] = matcher1.group(3);
		}
		return ret;
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

	private static long getDate(String key) throws Exception {
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
	
	public static void main (String[] args) {
		new RTCount(args);
		
		try {
			
			stopWatch("Starting... ");
			
			//input
			for (int dnum = 11; dnum <= 14; dnum++) {
				String dssXX = "dss" + String.format("%2d", dnum);
				File infrootFile = new File(infroot, dssXX); 
				
				File[] infpathList = infrootFile.listFiles();
				
				if (infpathList == null || infpathList.length == 0) continue; 
				
				//日付順にディレクトリを処理するようにする
				Arrays.sort(infpathList);
				
				//テストのため15日分だけやる
				File[] infpathListShort = new File[15];
				if (infpathList.length >= 15) {
					for (int i = 0; i<15; i++) {
						infpathListShort[i] =infpathList[i];
					}
				}else{
					infpathListShort = new File[infpathList.length];
					for (int i = 0; i<infpathListShort.length; i++) {
						infpathListShort[i] =infpathList[i];
					}
				}
				Arrays.sort(infpathListShort);
				
				for (File infpath : infpathListShort) {
					File[] fnameList = infpath.listFiles();
					
					stopWatch("Directory: " + infpath.toString());
	
					if (!retweeted.isEmpty()) retweeted.clear(); //初期化されていないならする
					for (File fname : fnameList) {
						stopWatch("Processing: " + fname.getName());
						
						InputStreamReader isr = new InputStreamReader(new FileInputStream(fname));
						BufferedReader br = new BufferedReader(isr);
						
						String line;
						while ((line = br.readLine()) != null) {
						
							String[] rawValues = getValues(line);
							ArrayList<String[]> retweetTimeAndURL = new ArrayList<String[]>();
							
							if (rawValues[0] != null) {
								if (retweeted.containsKey(rawValues[0])) retweetTimeAndURL = retweeted.get(rawValues[0]);
								String[] addition = {rawValues[1],rawValues[2]};
								retweetTimeAndURL.add(addition);
								retweeted.put(rawValues[0], retweetTimeAndURL);
							}
						}
						
						br.close();
						isr.close();
	
						stopWatch("Complete: " + fname.getName());
						
						//break;
					}
					
					resultWriter();
					
					retweeted.clear(); //ディレクトリ単位での処理が終了した時点でマップを破棄．以降はファイルの有無で既知・未知を判定
					
				}
				//テストのためdss11だけやる．
				break;
			}
			stopWatch("Done.");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}