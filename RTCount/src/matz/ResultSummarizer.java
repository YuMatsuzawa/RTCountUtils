package matz;

/* 結果ファイルを全部取りまとめてグラフ等作りやすくする。
 * 分類のための特徴ベクトルを抽出する。
 */

import java.io.*;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class ResultSummarizer {
	private static boolean vecAppend = false;
	private static boolean bicAppend = false;
	private static String infroot = "./";
	private static int ref = 2;
	
	public ResultSummarizer(String[] args) {
		try{
			if (args.length >= 1) {
				infroot = args[0];
				if (args.length >= 2) {
					ref = Integer.parseInt(args[1]);
				}
			}
		}catch(Exception e){
			System.out.println("Invalid argument.");
		}
	}
	
	//実行時間計測
	private static long start = System.currentTimeMillis();
	private static long split = start;
	private static void stopWatch(String comment) {
		long current = System.currentTimeMillis();
		System.out.println((current - start) + "\t" + (current - split) + "\t" + comment);
		split = current;
	}

	public static void main (String[] args) {
		new ResultSummarizer(args);
		stopWatch("Starting..");
		
		try{
			File infpath = new File(infroot);
			for (File file : infpath.listFiles()) {
				if (file.getName().matches("[\\d]+\\.res")){
					if (file.listFiles().length == 0) continue;
					int dlnLogNum = 1;
					int lnLogNum = 1;
					File comparator;
					File res_dln = new File(file, file.getName().replaceAll("([\\d]+\\.)(res)", "$1" + "res\\.dln\\." + dlnLogNum + "\\.csv"));
					File bic_dln = new File(file, file.getName().replaceAll("([\\d]+\\.)(res)", "$1" + "bic_dln\\." + dlnLogNum + "\\.csv"));
					while((comparator = new File(file,
							file.getName().replaceAll("([\\d]+\\.)(res)", "$1" + "res\\.dln\\." + dlnLogNum + "\\.csv"))).exists()){
						res_dln = comparator;
						bic_dln = new File(file, file.getName().replaceAll("([\\d]+\\.)(res)", "$1" + "bic_dln\\." + dlnLogNum + "\\.csv"));
						dlnLogNum++;
					}
					File res_ln = new File(file, file.getName().replaceAll("([\\d]+\\.)(res)", "$1" + "res\\.ln\\." + lnLogNum + "\\.csv"));
					File bic_ln = new File(file, file.getName().replaceAll("([\\d]+\\.)(res)", "$1" + "bic_ln\\." + lnLogNum + "\\.csv"));
					while((comparator = new File(file,
							file.getName().replaceAll("([\\d]+\\.)(res)", "$1" + "res\\.ln\\." + lnLogNum + "\\.csv"))).exists()){
						res_ln = comparator;
						bic_ln = new File(file, file.getName().replaceAll("([\\d]+\\.)(res)", "$1" + "bic_ln\\." + lnLogNum + "\\.csv"));
						lnLogNum++;
					}
					modelSummarizer(res_dln, res_ln, bic_dln, bic_ln);
				} else continue;
			}
			stopWatch("Done.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void modelSummarizer(File res_dln, File res_ln, File bic_dln, File bic_ln) throws Exception {
		String tweetURL = new String();
		double[] maxLikelihood = {0.0, 0.0};
		stopWatch("Reading: " + res_dln.getPath());

		try{
			InputStreamReader isr = new InputStreamReader(new FileInputStream(res_dln));
			BufferedReader br = new BufferedReader(isr);
			tweetURL = br.readLine();
			
			while(!br.readLine().isEmpty());

			ArrayList<double[]> clusters = new ArrayList<double[]>();
			String line = new String();
			while((line = br.readLine()).startsWith("#")) {
				double[] params = new double[4];
				line = br.readLine();
				params[0] = Double.parseDouble(line.substring(8)); //"weight: "
				line = br.readLine();
				params[1] = Double.parseDouble(line.substring(6)); //"mean: "
				line = br.readLine();
				params[2] = Double.parseDouble(line.substring(10)); //"variance: "
				line = br.readLine();
				params[3] = Double.parseDouble(line.substring(7)); //"delay: "
				br.readLine();
				clusters.add(params);
			}
			
			while((line = br.readLine()) != null){ //残る尤度列を全部読み取り
				maxLikelihood[0] = Double.parseDouble(line); //最終行を記録
			}
			
			
			br.close();
			isr.close();
			
			double[] charVector = new double[1 + 4*ref];
			charVector[0] = clusters.size();
			
			int[] weightMaxIndex = new int[ref];
			ArrayList<Integer> alrdyPicked = new ArrayList<Integer>();
			for(int r=0; r<ref; r++){
				double weightMin = 0.0;
				for (int k = 0; k < clusters.size(); k++) {
					double[] params = clusters.get(k);
					if(params[0] > weightMin && !alrdyPicked.contains(k)) {
						weightMin = params[0];
						weightMaxIndex[r] = k;
					}
				}
				for (int i=0; i<4; i++) {
					if(!alrdyPicked.contains(weightMaxIndex[r])) {
						double[] params = clusters.get(weightMaxIndex[r]);
						charVector[1 + 4*r + i] = params[i];
					}else{
						charVector[1 + 4*r + i] = 0.0;
					}
				}
				if(!alrdyPicked.contains(weightMaxIndex[r])) alrdyPicked.add(weightMaxIndex[r]);
			}

			if(res_ln.exists()){
				InputStreamReader isr2 = new InputStreamReader(new FileInputStream(res_ln));
				BufferedReader br2 = new BufferedReader(isr2);
				
				line = new String();
				while((line = br2.readLine()) != null){
					try{
						maxLikelihood[1] = Double.parseDouble(line);
					}catch(NumberFormatException e) {
						continue;
					}
				}
				
				br2.close();
				isr2.close();
			}

			File infrootFile = new File(infroot);
			File char_vec = new File(infrootFile, infrootFile.getName() + ".char_vec.csv");
			
			OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(char_vec, vecAppend));
			BufferedWriter bw = new BufferedWriter(osw); 
			if (!vecAppend) {
				stopWatch("Writing: "+ char_vec.getPath());
				bw.write("Tweet URL,# of Cluster");
				for(int r=0; r<ref; r++) {
					bw.write(String.format(",weight%d,mean%d,variance%d,delay%d",r,r,r,r));
				}
				bw.newLine();
				vecAppend = true;
			}else{
				stopWatch("Appending: "+ char_vec.getPath());
			}
			bw.write(tweetURL);
			for(int i=0; i<charVector.length; i++) bw.write("," + charVector[i]);
			bw.newLine();
			
			bw.close();
			osw.close();
		
		}catch(FileNotFoundException e){
			try{
			InputStreamReader isr = new InputStreamReader(new FileInputStream(res_ln));
			BufferedReader br = new BufferedReader(isr);
			tweetURL = br.readLine();
			String line = new String();
			while((line = br.readLine()) != null){
				try {
					maxLikelihood[1] = Double.parseDouble(line);
				}catch(NumberFormatException f) {
					continue;
				}
			}
			
			br.close();
			isr.close();
			}catch(Exception d){
				e.printStackTrace();
			}
		}catch (NumberFormatException e) {
			e.printStackTrace();
		}
				
		BICSummarizer(bic_dln, bic_ln, tweetURL, maxLikelihood);
	}

	private static void BICSummarizer(File bic_dln, File bic_ln, String tweetURL, double[] maxLikelihood) throws Exception {
		String cluster = "0";
		double dlnBIC = 0;
		double lnBIC = 0;
		File[] bics = {bic_dln, bic_ln};
		for (File file : bics) {
			try {
				InputStreamReader isr = new InputStreamReader(new FileInputStream(file));
				BufferedReader br = new BufferedReader(isr);
				
				stopWatch("Reading: " + file.getPath());
				String line = br.readLine(); //先頭行
				
				while((line = br.readLine()) != null) {
					StringTokenizer st = new StringTokenizer(line, ",");
					cluster = st.nextToken();
					if (file == bic_dln) dlnBIC = Double.parseDouble(st.nextToken());
					else lnBIC = Double.parseDouble(st.nextToken());
				}
				
				br.close();
				isr.close();
			} catch(FileNotFoundException e) {
				continue;
			}
		}
		
		File infrootFile = new File(infroot);
		File bic_summary = new File(infrootFile, infrootFile.getName() + ".bic_summ.csv");
		
		OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(bic_summary, bicAppend));
		BufferedWriter bw = new BufferedWriter(osw); 
		if (!bicAppend) {
			stopWatch("Writing: "+ bic_summary.getPath());
			bw.write("Tweet URL,# of Cluster,BIC - 3pLN,BIC - LN,BICdiff(%),MaxL - 3pLN,MaxL - LN,MaxLdiff");
			bw.newLine();
			bicAppend = true;
		}else{
			stopWatch("Appending: "+ bic_summary.getPath());
		}
		bw.write(tweetURL + "," + cluster + "," + dlnBIC + "," + lnBIC + ",");
		if(dlnBIC !=0 && lnBIC !=0) bw.write(String.format("%f", dlnBIC / lnBIC * 100 - 100));
		else bw.write("0");
		bw.write("," + maxLikelihood[0] + "," + maxLikelihood[1] + ",");
		if(maxLikelihood[0] !=0 && maxLikelihood[1] !=0) bw.write(String.format("%f", maxLikelihood[0] - maxLikelihood[1]));
		else bw.write("0");
		bw.newLine();
		
		bw.close();
		osw.close();
		
	}
}