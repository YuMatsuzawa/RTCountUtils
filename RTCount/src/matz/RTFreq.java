package matz;

/* �����̂��߂�RTCount�ō����RT���O�t�@�C������x�����z�����N���X�B
 * �t�@�C���T�C�Y�̂�����x�傫��RT���O�S�Ă����������B
 * ����ȂɎ��s���Ԃ͂�����Ȃ��B
 */

import java.io.*;
import java.util.HashMap;
import java.util.StringTokenizer;

public class RTFreq {
	//private static int resol = 10; //�x�����z�̉𑜓x�i���j
	private static int pitch = 10*60; //�x�����z�̉𑜓x�i�b�j
	private static String infroot = "./";
	public RTFreq(String[] args) {
		/* �R���X�g���N�^��main�̖`���ŌĂ΂�A�R�}���h���C������args���܂邲�Ǝ󂯂邪�A���ۂɂ͍ŏ���2�v�f�܂ł݂̂��g���B
		 * 1�v�f�����Ȃ��ꍇ�͂��ꂪ�x�����z�𑜓x(int)���p�X(String)����parse�̉ۂŔ��肷��B 
		 * 0�v�f�Ȃ�ǂ���̕ϐ����f�t�H���g�l���g���B
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

	private static void freq(File file) throws Exception { //�t�@�C������=�eretweet���Ƃɓx�����z���쐬���o��
		InputStreamReader isr = new InputStreamReader(new FileInputStream(file));
		BufferedReader br = new BufferedReader(isr);
		String head = br.readLine(); //�擪�s(URL)
		
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
			bw.write("Min = ," + min*pitch + ", Max = ," + max*pitch); //�O���t�`��p��Min-Max�𖾋L���Ă���
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
			long threshold = 1024; //���O�̃t�@�C���T�C�Y�ŁA�x�����z���쐬����ɒl���邾�������̃f�[�^�����郊�c�C�[�g�������������ʂ���B������R���X�g���N�g�ł���悤�ɂ��Ă������B
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