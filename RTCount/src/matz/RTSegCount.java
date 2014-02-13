package matz;

/* �����̂��߂�RTCount�ō����RT���O�t�@�C������x�����z�����N���X�B
 * �t�@�C���T�C�Y�̂�����x�傫��RT���O�S�Ă����������B
 * ����ȂɎ��s���Ԃ͂�����Ȃ��B
 */

import java.io.*;

public class RTSegCount {
	private static String infroot = "./";
	private static int upperMax = 10000;
	public RTSegCount(String[] args) {
		/* �R���X�g���N�^��main�̖`���ŌĂ΂�A�R�}���h���C������args���܂邲�Ǝ󂯂邪�A���ۂɂ͍ŏ���2�v�f�܂ł݂̂��g���B
		 * 1�v�f�����Ȃ��ꍇ�͂��ꂪ�x�����z�𑜓x(int)���p�X(String)����parse�̉ۂŔ��肷��B 
		 * 0�v�f�Ȃ�ǂ���̕ϐ����f�t�H���g�l���g���B
		 */
		if (args.length >= 1) {
			try{
				infroot = args[0];
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void count(File file) throws Exception { //�t�@�C������=�eretweet���Ƃɓx�����z���쐬���o��
		InputStreamReader isr = new InputStreamReader(new FileInputStream(file));
		BufferedReader br = new BufferedReader(isr);
		String head = br.readLine(); //�擪�s(URL)
		
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
			bw.write("Total RT = ," + totalCount + ", RT before " + upperMax +" = ," + segmentCount); //�O���t�`��p��Min-Max�𖾋L���Ă���
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