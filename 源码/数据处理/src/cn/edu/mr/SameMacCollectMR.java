package cn.edu.mr;

import java.io.IOException;
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
import org.apache.hadoop.mapreduce.lib.output.LazyOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import cn.edu.dao.GetStudentByMacDao;
import cn.edu.domain.MacBean;

/**
 * 
* @Description: 
*	����ͬ��mac��ַ�����ݣ�д����ѧ���������ļ�����
* @version: v1.0.0
* @author: liao
* @date: 2019��1��21�� ����2:28:19 
 */
public class SameMacCollectMR extends Configured implements Tool{
	
	private static HashMap<String, String> studentMap = new HashMap<>();
	
	static{
		studentMap = GetStudentByMacDao.getMacStudent();
	}
	
	public static class MacSortMapper extends Mapper<LongWritable, Text, Text, MacBean>{
		
		@Override
		protected void map(LongWritable key, Text value, Mapper<LongWritable, Text, Text, MacBean>.Context context)
				throws IOException, InterruptedException {
			
			//�õ�һ��
			String line = value.toString();
			//�и�
			String[] split_list = line.split(",");
			String mac = split_list[3];
			double range = Double.parseDouble(split_list[4]);
			String time = split_list[1];
			String addr = split_list[5];
			String ds = split_list[2];
			//��װ
			MacBean macBean = new MacBean(mac, range, time,addr,ds);
			context.write(new Text(mac),macBean);
		}
	}
	
	public static class MacSortReducer extends Reducer<Text, MacBean, MacBean, NullWritable>{
		//����һ����·�����
		private MultipleOutputs<MacBean, NullWritable> multipleOutput;
		
		@Override
		protected void setup(Reducer<Text, MacBean, MacBean, NullWritable>.Context context)
				throws IOException, InterruptedException {
			multipleOutput = new MultipleOutputs<MacBean, NullWritable>(context);
			super.setup(context);
		}

		@Override
		protected void reduce(Text key, Iterable<MacBean> values,
				Reducer<Text, MacBean, MacBean, NullWritable>.Context arg2) throws IOException, InterruptedException {
			for(MacBean macBean : values){
				if (studentMap.get(key.toString()) == null) {
					multipleOutput.write(macBean, NullWritable.get(), "notExist");
				}else{
					multipleOutput.write(macBean,NullWritable.get(), studentMap.get(key.toString()));
				}
			}
		}
		
		@Override
		protected void cleanup(Reducer<Text, MacBean, MacBean, NullWritable>.Context context)
				throws IOException, InterruptedException {
			multipleOutput.close();
			super.cleanup(context);
		}
	}
	
		@Override
		public int run(String[] arg0) throws Exception {
			
			Configuration configuration = new Configuration();
			Job job = Job.getInstance(configuration);
			
			job.setJarByClass(SameMacCollectMR.class);
			
			//���� Mapper��Reducer
			job.setMapperClass(MacSortMapper.class);
			job.setReducerClass(MacSortReducer.class);
			
			//���÷���
//			job.setPartitionerClass(MacPartitioner.class);
			
			//����Reducer����������������������ٸ��ļ�
//			job.setNumReduceTasks(7);
			
			//ȥ���յ�����ļ�
			LazyOutputFormat.setOutputFormatClass(job, TextOutputFormat.class);
			
			//����Mapper��Reducer�����
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(MacBean.class);
			
			//�����������ݵ�·����������ݵ�·��
			Path inputpath = new Path(arg0[0]);
			Path outputpath = new Path(arg0[1]);
			//������·���Ѿ����ڣ�����ɾ��
			FileSystem fs = FileSystem.get(configuration);
			if(fs.exists(outputpath)){
				fs.delete(outputpath,true);
			}
			
			FileInputFormat.setInputPaths(job, inputpath);
			FileOutputFormat.setOutputPath(job, outputpath);
			
			return job.waitForCompletion(true) ? 1 : 0;
		}
	
	public static void main(String[] args) throws Exception {
		if (args == null || args.length < 2) {
			System.err.println("��������ʹ�ò���������·�� + ���·��");
			System.exit(-1);
		}
		int res  = ToolRunner.run(new Configuration(), new SameMacCollectMR(),args);
		System.exit(res);
		
	}


}
