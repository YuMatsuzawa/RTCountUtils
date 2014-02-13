package matz;

/* ���o���������x�N�g�����N���X�^�����O���āA�������f���𓱂�
 * �������f���̋ߎ��Ȑ�������
 */

import java.io.*;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class Classifier {
	private static String infroot = "./";
	private static int cluster = 3;
	private static int maxIter = 100;
	private static int ref = 2;
	
	public Classifier(String[] args) { //constructor
		try{
			if (args.length >= 1) {
				infroot = args[0];
				if (args.length >= 2) {
					cluster = Integer.parseInt(args[1]);
					if (args.length >= 3) {
						maxIter = Integer.parseInt(args[2]);
						if (args.length >= 4) {
							ref = Integer.parseInt(args[3]);
						}
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
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
		new Classifier(args);
		
		try{
			File infpath = new File(infroot);
			File charVecFile = new File(infroot, infpath.getName() + ".char_vec.csv"); //infroot�ɂ�summarizedResult�̂���f�B���N�g�����w��
			File resFile = new File(infroot, infpath.getName() + ".models." + cluster + ".csv");
			
			int vecLen = 1 + 4*ref;
			
			InputStreamReader isr = new InputStreamReader(new FileInputStream(charVecFile));
			BufferedReader br = new BufferedReader(isr);
			
			ArrayList<double[]> charVecList = new ArrayList<double[]>();		
			String line = br.readLine(); //�擪�s			
			while((line = br.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line, ",");
				st.nextToken();
				int index = 0;
				double[] tmpCharVec = new double[vecLen];
				while(st.hasMoreTokens()){
					tmpCharVec[index] = Double.parseDouble(st.nextToken());
					index++;
				}
				charVecList.add(tmpCharVec);
			}
			br.close();
			isr.close();
			
			double[][] charVec = new double[charVecList.size()][vecLen];
			for(int i = 0; i<charVecList.size(); i++) {
				charVec[i] = charVecList.get(i);
			}
			int size = charVec.length;
			
			double[] vecMeans = new double[vecLen];
			double[] vecDevs = new double[vecLen];
			double[][] charVecNorm = new double[size][vecLen];
			for(int j = 0; j<vecLen; j++) {
				for(int i = 0; i<size; i++) {
					vecMeans[j] += charVec[i][j];
				}
			}
			for(int j = 0; j<vecLen; j++) vecMeans[j] /= size;
			for(int j = 0; j<vecLen; j++) {
				for(int i = 0; i<size; i++) {
					double gap = charVec[i][j] - vecMeans[j];
					vecDevs[j] += Math.sqrt(gap*gap);
				}
			}
			for(int j = 0; j<vecLen; j++) vecDevs[j] /= size;
			
			double[] charVecNormMax = new double[vecLen];
			double[] charVecNormMin = new double[vecLen];
			for(int j = 0; j<vecLen; j++) {
				charVecNormMax[j] = 0.0;
				charVecNormMin[j] = Double.MAX_VALUE;
			}
			for(int j = 0; j<vecLen; j++) {
				for(int i = 0; i<size; i++) {
					charVecNorm[i][j] = (charVec[i][j] - vecMeans[j]) / vecDevs[j];
					if (charVecNorm[i][j] > charVecNormMax[j]) charVecNormMax[j] = charVecNorm[i][j];
					if (charVecNorm[i][j] < charVecNormMin[j]) charVecNormMin[j] = charVecNorm[i][j];
				}
			}
			
			double[][] classifiedModels = new double[cluster][vecLen];
			int[] clusterIndex = new int[size];
			clusterIndex = kmeans(cluster, charVecNorm, charVecNormMax, charVecNormMin, maxIter);
			
			//cluserIndex����classifiedModels�i�N���X�^�d�S�j���o��
			int[] dataNum = new int[cluster];
			for(int k=0; k<cluster; k++) {
				for(int j=0; j<vecLen; j++) {
					classifiedModels[k][j] = 0.0;
				}
			}
			for(int i=0; i<size; i++) {
				int index = clusterIndex[i];
				for(int j=0; j<vecLen; j++) {
					classifiedModels[index][j] += charVecNorm[i][j];
				}
				dataNum[index]++;
			}
			for(int k=0; k<cluster; k++) {
				for(int j=0; j<vecLen; j++) {
					classifiedModels[k][j] /= dataNum[k];
				}
			}
			
			for(int k=0; k<cluster; k++) {
				for(int j=0; j<vecLen; j++) {
					classifiedModels[k][j] = classifiedModels[k][j] * vecDevs[j] + vecMeans[j];
				}
			}
			
			//output
			resultPlotter(resFile, dataNum, classifiedModels);
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void resultPlotter(File resFile, int[] dataNum, double[][] classifiedModels) throws Exception{
		stopWatch("Writing: " + resFile.toString());
		//���ʏo��
		OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(resFile));
		BufferedWriter bw = new BufferedWriter(osw);
		bw.write("Time");
		for(int k=0; k<cluster; k++){
			for(int i=0; i<ref; i++) {
				bw.write(String.format(",Class %d-%d", k, i));
			}
			bw.write(String.format(",Model %d,", k)); // +","
		}
		bw.newLine();
		
		//�ߎ��Ȑ��v���b�g�����E�o��
		int range = 10000;
		int pitch = 10;
		int resol = range/pitch;
		for (int r=0; r<resol; r++) {
			int time = pitch*r;
			bw.write(String.format("%d", time));
			for (int k=0; k<cluster; k++) {
				double plot = 0.0;
				double subPlot = 0.0;
				for (int i=0; i<ref; i++) {
					subPlot = classifiedModels[k][1 + 4*i] * delayedLognormal(time, classifiedModels[k][1 + i*4 + 3],
							classifiedModels[k][1 + 4*i + 1], classifiedModels[k][1 + 4*i + 2]);
					bw.write(String.format(",%f", subPlot));
					plot += subPlot;
				}
				bw.write(String.format(",%f,", plot)); // +","
			}
			bw.newLine();
		}
		bw.newLine();
		
		//�N���X�^�����o��
		for(int k=0; k<cluster; k++) {
			bw.write(String.format("#Model %d, mixture: %f, %d", k, classifiedModels[k][0], dataNum[k]));
			bw.newLine();
			for(int i=0; i<ref; i++) {
				bw.write(String.format("#Class %d-%d", k, i));
				bw.newLine();
				bw.write(String.format("weight: %f", classifiedModels[k][1 + 4*i]));
				bw.newLine();
				bw.write(String.format("mean: %f", classifiedModels[k][1 + 4*i + 1]));
				bw.newLine();
				bw.write(String.format("variance: %f", classifiedModels[k][1 + 4*i + 2]));
				bw.newLine();
				bw.write(String.format("delay: %f", classifiedModels[k][1 + 4*i + 3]));
				bw.newLine();
			}
			bw.newLine();
		}
		
		bw.close();
		osw.close();
		
	}
	
	private static double delayedLognormal(int time, double tau, double mean, double var) {
		double delay = time - tau;
		if(delay <= 0 || var == 0) return 0.0;
		else if(delay < 1) delay = 1;
		double denom = Math.sqrt(2*Math.PI*var) * delay;
		double gap = Math.log(delay) - mean;
		double ret = Math.exp(-0.5*gap*gap/var)/denom; 
		if(!Double.isNaN(ret)) return ret;
		else return 0.0;
	}

	public static int[] kmeans(int cluster, double[][] data, double[] dataMax, double[] dataMin, int max_itr) {//k���ϖ@�B�e�f�[�^�_���ŏ��ɑ�����N���X�^�̃C���f�b�N�X��l�Ƃ��Ď��z���Ԃ��B
		stopWatch("Clustering by K-means method..");
		
		int size = data.length;
		int vecLen = 1 + 4*ref;
		int[] clusterIndex = new int[size]; //�e�_�̔z����
		double[][] centerModels = new double[cluster][vecLen]; //�e�N���X�^���S�̃p�����[�^
		
		//�����_������U��
		for (int i=0; i<size; i++) clusterIndex[i] = (int)(cluster * Math.random());
		
		//�C�e���[�^
		for (int itr=0; itr<max_itr; itr++) {
			
			//��ɒ��S�Čv�Z
			int[] dataNum = new int[cluster];
			for(int k=0; k<cluster; k++) {
				for(int j=0; j<vecLen; j++) {
					centerModels[k][j] = 0.0;
				}
			}
			for(int i=0; i<size; i++) {
				int index = clusterIndex[i];
				for(int j=0; j<vecLen; j++) {
					centerModels[index][j] += data[i][j];
				}
				dataNum[index]++;
			}
			for(int k=0; k<cluster; k++) {
				for(int j=0; j<vecLen; j++) {
					centerModels[k][j] /= dataNum[k];
				}
			}
			
			//�ł����S�̋߂��N���X�^�Ƀf�[�^��z��
			for(int i=0; i<size; i++) {
				int index = 0;
				double minval = Double.MAX_VALUE;
				for(int k=0; k<cluster; k++){
					double dist = 0.0;
					for(int j=0; j<vecLen; j++) {
						double gap = data[i][j] - centerModels[k][j];
						dist += gap * gap;
					}
					dist = Math.sqrt(dist);
					if(dist < minval) {
						minval = dist;
						index = k;
					}
				}
				clusterIndex[i] = index;
			}
		}
		
		return clusterIndex;
	}
}



