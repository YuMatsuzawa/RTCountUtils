package matz;

/* RT���O�̒���RT�������XXX�ʂ�Tweet���X�g�����
 * �Ƃ肠�����t�@�C���J�����s���J�E���g�����[�v���\�[�g�����X�g��
 * ���̉ߒ��Ńf�[�^�̗ݐϕp�x���z�����
 * ���z�̃h���C����4�����ŁA1����1440����1��ԂƂ��ĕ��z������
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
	private static int pick = 1000; //�s�b�N���鐔
	private static String infroot = "./";
	
	public RTPicker(String[] args) {
		/* �R���X�g���N�^��main�̖`���ŌĂ΂�A�R�}���h���C������args���܂邲�Ǝ󂯂邪�A���ۂɂ͍ŏ���2�v�f�܂ł݂̂��g���B
		 * 1�v�f�����Ȃ��ꍇ�͂��ꂪ���o��(int)���p�X(String)����parse�̉ۂŔ��肷��B 
		 * 0�v�f�Ȃ�ǂ���̕ϐ����f�t�H���g�l���g���B
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

	private static HashMap<File, Integer> retweetCount = new HashMap<File, Integer>(); //�t�@�C������RT���𐔂���}�b�v
	//private static int[] accumFreq = new int[122];
	private static int fourMonths = 122;
	private static int minuteaDay = 1440; //1��1440�� 
	private static int secondsaDay = 1440*60;

	private static int RTcountMin = Integer.MAX_VALUE;
	private static int RTcountMax = 0;
	
	public static int[] accum(File file, int[] accumFreq) throws Exception { //1�����݂̗ݐϕp�x���z(4������122���܂�)�����
		InputStreamReader isr = new InputStreamReader(new FileInputStream(file));
		BufferedReader br = new BufferedReader(isr);
		String line = br.readLine(); //�擪�s(URL)
		
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

	
	private static URL checkRedirect(URL queryURL) throws Exception { //���_�C���N�g���I���܂ōċA�I�Ƀ��_�C���N�g��ɐڑ����J��
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
		/* Web����I���W�i���c�C�[�g�̓������擾���Ă���
		 * URL�N���X�ƁA��������X�g���[�����J�����\�b�h���g�p
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
	
	private static void RTcounter(File file) throws Exception { //�����邾��
		InputStreamReader isr = new InputStreamReader(new FileInputStream(file));
		BufferedReader br = new BufferedReader(isr);
		String line = br.readLine(); //�擪�s(URL)
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
		ArrayList<Map.Entry<File, Integer>> RTList = new ArrayList<Map.Entry<File, Integer>>(retweetCount.entrySet()); //Map�̓��e��Value�Ń\�[�g����List�Ɉڂ��ւ�
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
			File infpath = new File(infroot); //infroot�ɂ̓f�[�^�̂���f�B���N�g�����̂��̂��w�肷��
			int[] accumFreq = new int[fourMonths];
			for (int i=0; i<fourMonths; i++) accumFreq[i] = 0;
			for (File file : infpath.listFiles()) {
				if (file.getName().matches("[\\d]+.csv")){ //�f�[�^�͐�����.csv�`��
					try {
						//accumFreq = accum(file, accumFreq);	//�t�@�C������RT�𐔂��ݐϕp�x���z�����
						RTcounter(file);						//�����邾��
					} catch (FileNotFoundException e) {
						continue;
					}
				} else continue;
			}
			//�S�t�@�C�������I����
			
			//�ݐϕp�x���z�o��
			//accumWrite(accumFreq);
			
			//RT�����N�t��&���X�g���
			RTRank();
		
		
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}



