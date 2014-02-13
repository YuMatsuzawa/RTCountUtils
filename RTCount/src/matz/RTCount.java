package matz;

/* ����RT��retweet_url�ɒl�����x�����Ă������Ő����グ��B
 * �����������̃N���X��dssXX�f�B���N�g����ɑ΂�8-9���Ԃ�����B
 * 	$ java -jar RTCount.jar
 * �Ń��C���N���X������ɑ���B�ǉ�����RTFreq���g���Ƃ���
 * 	$ java -cp RTCount.jar matz.RTFreq
 * �ŃN���X�w�肵�Ď��s����B�o�b�N�O���E���h�v���Z�X�ɂ��āA�����O�A�E�g������s������ɂ�nohup���g���B
 * 	$ nohup java <option> <jarfile> [<class>[ <args>]] > RTCount.log 2> err.log &
 * ������&���o�b�N�O���E���h�w��q�Bargs�ɂ̓p�X���邢�̓��O臒l�i�܂��͗����j���󂯂���B
 * �o�O��������nohup�ŊJ�n�����������E�������Ȃ�����Akill -kill <pid>�Ŏ~�߂���Bpid��$ ps��$ jps�Œ��ׂ�B
 * dss11-14�܂őS�ď�������B�����A���݂��Ȃ�or��ȃf�B���N�g���̓X�L�b�v����B
 * RTCount��RTFreq�����ʂȊO���p�b�P�[�W���g���Ă��Ȃ��̂Ńl�C�e�B�ujava��������T�[�o�Ȃ炻�̂܂ܓ����͂��B
 * build.jardesc��ant�ł͂Ȃ����r���h�t�@�C���Ȃ̂ł�����_�u�N�����Ď��֘A�łŐݒ�s�v�B
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
		/* �R���X�g���N�^��main�̖`���ŌĂ΂�A�R�}���h���C������args���܂邲�Ǝ󂯂邪�A���ۂɂ͍ŏ���2�v�f�܂ł݂̂��g���B
		 * 1�v�f�����Ȃ��ꍇ�͂��ꂪ���O臒l(long)���p�X(String)���𔻒肷��B 
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
	
	//���s���Ԍv��
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
	
	/* key�F�I���W�i��tweet��URL<String>,value�Fretweet���ꂽ���Ԃ�URL�̔z���S�Ď���ArrayList
	 * ���̃}�b�v�͏����������߂邲�ƂɃN�\�ł����Ȃ��Ă���((key�̐�=retweet���ꂽ���Ƃ̂���URL�̐�)*(value��size=retweet���ꂽ��))
	 * �K�؂ȃ^�C�~���O��clear()���Ă�Ń��������J�����邱�ƁB
	 */
	private static HashMap<String, ArrayList<String[]>> retweeted = new HashMap<String, ArrayList<String[]>>();
	private static void resultWriter() throws IOException, ParseException { //retweet��I�ʂ��ă��O�o�͂���
		File outfpath = new File("retweetLog" + start);
		if (!outfpath.isDirectory()) outfpath.mkdir();
		
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		retweetedNum = retweeted.size();
		checkedNum = 0;
		resultWriteTimeSum = 0;
		for(Entry<String, ArrayList<String[]>> set : retweeted.entrySet()) {
			
			String fname = set.getKey().replaceAll("http://(([^/]+)/)+", ""); //���O�t�@�C����
			File outf = new File(outfpath, fname + ".csv");
			
			/* ���O�Ɏc��������
			 * (1)1���ӂ�XXX���c�C�[�g�ȏ�
			 * (2)���ɂ��̃��c�C�[�g�̃��O������ i.e.(1)���ߋ���1��ȏ㖞�������L�^������
			 * �̂����ꂩ 
			 */
			checkedNum++;
			
			if (outf.exists() || set.getValue().size() >= logThreshold) { 
				
				Date originalDate;
				try {
					originalDate = new Date(1000 * getDate(set.getKey())); //web�\�[�X�Ō�����Unix���Ԃ͕b�Ȃ̂�1000�{���ă~���b�ɂ��Ă���
				} catch (Exception e) { //web�\�[�X����ŗ�O���������Ă�Abort���Ď��ɍs���B�^�C���A�E�g�Ƃ�data-time��������Ȃ��Ƃ��B
					stopWatch("Aborting: " + set.getKey());
					continue;
				}
				
				OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(outf, true)); //append=true�ɂ��Ă����ΒǋL				
				BufferedWriter bw = new BufferedWriter(osw);


				if (outf.length() == 0) {
					stopWatch("Writing: " + outf.toString());
					bw.write(set.getKey()); //�ŏ�������URL�������B
					bw.newLine();
				} else stopWatch("Appending: " + outf.toString());
								
				for(String[] rawDateAndURL : set.getValue()) {
					Date retweetDate = df.parse(rawDateAndURL[0]);
					long expiredSec = (retweetDate.getTime() - originalDate.getTime()) / 1000;
					//String expiredMin = String.format("%d", expiredSec/60);
					bw.write(String.format("%d, %s", expiredSec, rawDateAndURL[1])); //�f�[�^�𕪂ɒ������Ɏ擾����D
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

	private static long getDate(String key) throws Exception {
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
				
				//���t���Ƀf�B���N�g������������悤�ɂ���
				Arrays.sort(infpathList);
				
				//�e�X�g�̂���15�����������
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
	
					if (!retweeted.isEmpty()) retweeted.clear(); //����������Ă��Ȃ��Ȃ炷��
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
					
					retweeted.clear(); //�f�B���N�g���P�ʂł̏������I���������_�Ń}�b�v��j���D�ȍ~�̓t�@�C���̗L���Ŋ��m�E���m�𔻒�
					
				}
				//�e�X�g�̂���dss11�������D
				break;
			}
			stopWatch("Done.");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}