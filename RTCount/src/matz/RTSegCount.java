package matz;

/* 可視化のためにRTCountで作ったRTログファイルから度数分布を作るクラス。
 * ファイルサイズのある程度大きいRTログ全てが処理される。
 * そんなに実行時間はかからない。
 */

import java.io.*;

public class RTSegCount {
	private static String infroot = "./";
	private static int upperMax = 10000;
	public RTSegCount(String[] args) {
		/* コンストラクタはmainの冒頭で呼ばれ、コマンドライン引数argsをまるごと受けるが、実際には最初の2要素までのみを使う。
		 * 1要素しかない場合はそれが度数分布解像度(int)かパス(String)かをparseの可否で判定する。 
		 * 0要素ならどちらの変数もデフォルト値を使う。
		 */
		if (args.length >= 1) {
			try{
				infroot = args[0];
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void count(File file) throws Exception { //ファイルごと=各retweetごとに度数分布を作成し出力
		InputStreamReader isr = new InputStreamReader(new FileInputStream(file));
		BufferedReader br = new BufferedReader(isr);
		String head = br.readLine(); //先頭行(URL)
		
		int totalCount = 0;
		int segmentCount = 0;
		
		String line = new String();
		while((line = br.readLine()) != null) {
			int lineInt = Integer.parseInt(line);
			totalCount++;
			if(lineInt <= upperMax) segmentCount++;
		}
		br.close();
		isr.close();
			
		File outFile = new File(file.getPath().replaceAll("\\.csv", "\\.res"), file.getName().replaceAll("\\.csv", "\\.cnt\\.csv"));
		OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(outFile));
		BufferedWriter bw = new BufferedWriter(osw);
		if (outFile.length() == 0) {
			bw.write(head);
			bw.newLine();
			bw.write("Total RT = ," + totalCount + ", RT before " + upperMax +" = ," + segmentCount); //グラフ描画用にMin-Maxを明記しておく
			bw.newLine();
		}
		System.out.println("Complete: " + outFile.getPath());
		bw.close();
		osw.close();		
		
	}
	
	public static void main (String[] args) {
		new RTSegCount(args);
		
		try{
			File infpath = new File(infroot);
			for (File file : infpath.listFiles()) {
				if (file.getName().matches("[\\d]+.csv")){
					try {
						count(file);
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