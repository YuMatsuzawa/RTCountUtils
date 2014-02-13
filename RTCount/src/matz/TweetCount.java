package matz;

/* ����1���̃c�C�[�g����S��������B
 */

import java.io.*;
import java.lang.System;

public class TweetCount {
	private static String infroot = "decoded/hotlink/"; 
	public TweetCount(String[] args) { 
		/* �R���X�g���N�^��main�̖`���ŌĂ΂�A�R�}���h���C������args���܂邲�Ǝ󂯂邪�A���ۂɂ͍ŏ���2�v�f�܂ł݂̂��g���B
		 * 1�v�f�����Ȃ��ꍇ�͂��ꂪ���O臒l(long)���p�X(String)���𔻒肷��B 
		 */
		if (args.length >= 2) {
			try {
				infroot = args[1];
			} catch (NumberFormatException e) {
				infroot = args[0];
			}
		} else if (args.length == 1) {
			try{
			} catch (NumberFormatException e) {
				infroot = args[0];
			}
		}
	}
	
	//���s���Ԍv��
	private static long start = System.currentTimeMillis();
	private static long split = start;
	private static void stopWatch(String comment) {
		long current = System.currentTimeMillis();
		System.out.println((current - start) + "\t" + (current - split) + "\t" + comment);
		split = current;
	}

	public static void main (String[] args) {
		new TweetCount(args);
		
		try {
			
			stopWatch("Starting... ");
			
			//input
			String targetDate = new String();
			long tweetCount = 0;
			for (int dnum = 11; dnum <= 14; dnum++) {
				String dssXX = "dss" + String.format("%2d", dnum);
				File infrootFile = new File(infroot, dssXX); 
				
				File[] infpathList = infrootFile.listFiles();
				
				if (infpathList == null || infpathList.length == 0) continue;
				
				long dirCount = 0;
				
				for (File infpath : infpathList) {
					File[] fnameList = infpath.listFiles();
					
					if(targetDate.isEmpty()) targetDate = infpath.getName();
					
					if(targetDate.equals(infpath.getName())) {
					
					stopWatch("Directory: " + infpath.toString());
					
					for (File fname : fnameList) {
						stopWatch("Processing: " + fname.getName());
						
						InputStreamReader isr = new InputStreamReader(new FileInputStream(fname));
						BufferedReader br = new BufferedReader(isr);
						
						while (br.readLine() != null) {
							dirCount++;
						}
						
						br.close();
						isr.close();
	
						stopWatch("Complete: " + fname.getName());						
					}
					
					tweetCount += dirCount;
					break;
					
					} else {
						continue;
					}
				}
				
				stopWatch(infrootFile.getName() + " done: " + dirCount + "\n\t\tNumber of Tweets on " + targetDate + ": " + tweetCount);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}