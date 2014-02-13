package matz;

/* 可視化のためにRTCountで作ったRTログファイルから度数分布を作るクラス。
 * ファイルサイズのある程度大きいRTログ全てが処理される。
 * そんなに実行時間はかからない。
 */

import java.io.*;
import java.util.HashMap;
import java.util.StringTokenizer;

public class RTFreq {
	//private static int resol = 10; //度数分布の解像度（分）
	private static int pitch = 10*60; //度数分布の解像度（秒）
	private static String infroot = "./";
	public RTFreq(String[] args) {
		/* コンストラクタはmainの冒頭で呼ばれ、コマンドライン引数argsをまるごと受けるが、実際には最初の2要素までのみを使う。
		 * 1要素しかない場合はそれが度数分布解像度(int)かパス(String)かをparseの可否で判定する。 
		 * 0要素ならどちらの変数もデフォルト値を使う。
		 */
		if (args.length >= 2) {
			try {
				pitch = Integer.parseInt(args[0]);
				infroot = args[1];
			} catch (NumberFormatException e) {
				pitch = Integer.parseInt(args[1]);
				infroot = args[0];
			}
		} else if (args.length == 1) {
			try{
				pitch = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				infroot = args[0];
			}
		}
	}

	private static void freq(File file) throws Exception { //ファイルごと=各retweetごとに度数分布を作成し出力
		InputStreamReader isr = new InputStreamReader(new FileInputStream(file));
		BufferedReader br = new BufferedReader(isr);
		String head = br.readLine(); //先頭行(URL)
		
		int max = 0;
		int min = Integer.MAX_VALUE;
		
		HashMap<Integer, Integer> logMap = new HashMap<Integer, Integer>();
		String line = new String();
		int key;
		while((line = br.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(line, ",");
			int lineInt = Integer.parseInt(st.nextToken());

			key = lineInt/pitch;
			if (key > max) max = key;
			if (key < min) min = key;
			int value;
			if (logMap.containsKey(key)) {
				value = logMap.get(key);
				value++;
				logMap.put(key, value);
			} else {
				logMap.put(key, 1);
			}
		}
		br.close();
		isr.close();
			
		File outFile = new File(file.getPath().replaceFirst("\\.csv", "\\.freq.csv"));
		OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(outFile));
		BufferedWriter bw = new BufferedWriter(osw);
		if (outFile.length() == 0) {
			bw.write(head);
			bw.newLine();
			bw.write("Min = ," + min*pitch + ", Max = ," + max*pitch); //グラフ描画用にMin-Maxを明記しておく
			bw.newLine();
		}
		for (int i = min; i <= max; i++) {
			if (logMap.containsKey(i)) bw.write(String.format("%d,%d", i*pitch, logMap.get(i)));
			else bw.write(String.format("%d,%d", i*pitch, 0));
			bw.newLine();
		}
		System.out.println("Complete: " + outFile.getName());
		bw.close();
		osw.close();		
		
	}
	
	public static void main (String[] args) {
		new RTFreq(args);
		
		try{
			File infpath = new File(infroot);
			long threshold = 1024; //ログのファイルサイズで、度数分布を作成するに値するだけ多くのデータがあるリツイートをだいたい判別する。これをコンストラクトできるようにしてもいい。
			for (File file : infpath.listFiles()) {
				if (file.getName().matches("[\\d]+.csv") && file.length() >= threshold){
					try {
						freq(file);
						//break;
					} catch (FileNotFoundException e) {
						continue;
					}
				} else continue;
			}
		
		
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}