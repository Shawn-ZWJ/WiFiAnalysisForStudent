package cn.edu.mr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import cn.edu.utils.TimeFormatUtil;

/**
 * 
* @Description: 
* 	ds�ֶΣ�YΪϢ����NΪ��Ϣ��
*	�õ�һ��ѧ����Ϣ����ʱ��κ�ʱ�䳤
*	����� mac,startTime,endTime,differMinute,ds
* @version: v1.0.0
* @author: liao
* @date: 2019��2��21�� ����10:18:30 
 */
public class GetDsTimeMR extends Configured implements Tool{

	
	public static class GetDsTimeMapper extends Mapper<LongWritable, Text, Text, NullWritable>{
		
		private ArrayList<HashMap<String, String>> arrayList = new ArrayList<>();
		private String mac;
		
		@Override
		protected void map(LongWritable key, Text value, Mapper<LongWritable, Text, Text, NullWritable>.Context context)
				throws IOException, InterruptedException {
			String line = value.toString();
			String[] splits = line.split(",");
			mac = splits[0];
			String ds = splits[4];
			String time = splits[2];
			
			HashMap<String, String> hashMap = new HashMap<>();
			hashMap.put(time, ds);
			arrayList.add(hashMap);
			
		}
		
		@Override
		protected void cleanup(Mapper<LongWritable, Text, Text, NullWritable>.Context context)
				throws IOException, InterruptedException {

			//�����洢ʱ��Ե��±�
			ArrayList<String> abList = new ArrayList<>();
			for(int i = 0; i < arrayList.size(); i++){
				HashMap<String, String> hashMap = arrayList.get(i);
				String ds = hashMap.values().toString();
				for(int j = i+1; j <= arrayList.size(); j++){
					String ds2;
					if (j == arrayList.size()) {
						ds2 = "last";
					}else{
						HashMap<String, String> hashMap2 = arrayList.get(j);
						ds2 = hashMap2.values().toString();
					}
					if (!ds2.equals(ds)) {
						abList.add(i+"-"+(j-1));
						i = j - 1;
						break;
					}
				}
			}
			
			for (String str : abList) {
				int startTimeIndex = Integer.parseInt(str.split("-")[0]);
				int endTimeIndex = Integer.parseInt(str.split("-")[1]);
				HashMap<String, String> startHashMap = arrayList.get(startTimeIndex);
				HashMap<String, String> endHashMap = arrayList.get(endTimeIndex);
				String startTime = startHashMap.keySet().toString().replace("[", "").replace("]","");
				String endTime = endHashMap.keySet().toString().replace("[", "").replace("]","");
				String ds = endHashMap.values().toString().replace("[", "").replace("]","");
				int differMinute = TimeFormatUtil.getMinute(endTime) - TimeFormatUtil.getMinute(startTime);
				
				//Ϣ������Ϣ������3���ӵĲ���
				if (differMinute >= 3) {
					StringBuffer sb = new StringBuffer();
					sb.append(mac + "," +startTime + "," + endTime + "," + differMinute+ "," + ds);
					context.write(new Text(sb.toString()),NullWritable.get());
				}
			}
			
		}
	}
	
	public static class GetDsTimeReducer extends Reducer<Text, Text, Text, NullWritable>{
		@Override
		protected void reduce(Text key, Iterable<Text> values, Reducer<Text, Text, Text, NullWritable>.Context context)
				throws IOException, InterruptedException {
			context.write(key, NullWritable.get());
		}
	}
	
	@Override
	public int run(String[] arg0) throws Exception {
		Configuration configuration = new Configuration();
		Job job = Job.getInstance();
		
		job.setJarByClass(GetDsTimeMR.class);
		
		job.setMapperClass(GetDsTimeMapper.class);
		job.setReducerClass(GetDsTimeReducer.class);
		
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(NullWritable.class);
		
		Path inputpath = new Path(arg0[0]);
		Path outputpath = new Path(arg0[1]);
		
		FileSystem fs = FileSystem.get(configuration);
		if (fs.exists(outputpath)) {
			fs.delete(outputpath,true);
		}
		
		FileInputFormat.setInputPaths(job, inputpath);
		FileOutputFormat.setOutputPath(job, outputpath);
		
		return job.waitForCompletion(true) ? 1 : 0;
	}
	
	public static void main(String[] args) throws Exception {
		if (args == null || args.length < 2) {
			System.err.println("�������Ӧ���ǣ�����·�� + ���·��");
			System.exit(-1);
		}
		int res = ToolRunner.run(new Configuration(), new GetDsTimeMR(), args);
		System.exit(res);
	}
}
