package AMGraphTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sound.midi.SysexMessage;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class AMGraph {

	private static final float Thres_WR = (float) 0.2;
	private static final float NA_WR = (float) 0.8;
	private static final float Th_OS = (float) 0.2;//匹配阈值
	private Map<String, Integer> vexsMap = new HashMap<String, Integer>();// 点集
	private int[][] arcs = null; // 边集
	private Map<String, List<String>> nodeNotes = new HashMap<String, List<String>>();// 节点—注释
	private Map<String, Integer> Note = new HashMap<String, Integer>();// 注释出现次数
	private List<String> Notes = new ArrayList<String>(); // 注释库

	private Map<String, Integer> AdjacetNode = new HashMap<String, Integer>();// 一阶邻居节点(度)
	private int[][] CommonNode = null;// 共同邻居节点
	private int[][] UnionNode = null;// 并集邻居节点
	private float[][] t_sims = null;// 拓扑结构相似性

	private float[][] s_sims = null;// 语义相似性
	private float[][] weights = null;// 权值
	public float Th = 0;// 自适应阈值
	private float Recall = 0;// 回召率
	private float Precision = 0;// 准确率
	private float F_measure = 0;// 综合评价指标
	private Map<String, Float> Matrix = new HashMap<String, Float>();// 节点平均加权度
	private List<String> Seeds = new ArrayList<String>(); // 种子节点队列
	private List<List<String>> PC = new ArrayList<List<String>>(); // 算法识别复合物集合
	private List<List<String>> CYC = new ArrayList<List<String>>(); // 标准复合物集合

	public AMGraph(int n, File file) {

		int number = 0;
		this.arcs = new int[n][n];
//		for (int i = 0; i < n; i++) { // 初始化边集
//			for (int j = 0; j < n; j++) {
//				this.arcs[i][j] = 0; // 0表示该位置所对应的两顶点之间没有边
//			}
//		}
		try {
			// 加载点集
			InputStream io = new FileInputStream(file.getAbsoluteFile());
			XSSFWorkbook workbook = new XSSFWorkbook(io);
			Sheet sheet = workbook.getSheetAt(1);

			int firstrow = sheet.getFirstRowNum();
			int lastrow = sheet.getLastRowNum();

			for (int i = firstrow; i < lastrow + 1; i++) {
				Row row = sheet.getRow(i);
				if (row != null) {
					Cell cell = row.getCell(0);
					if (cell != null) {
						String string = cell.toString();
						vexsMap.put(string, number++);
					}
				}
				io.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			// 加载边集
			// 记录两端点的位置
			int vex1Site = 0;
			int vex2Site = 0;
			// 记录输入两端点
			String vex1 = null;
			String vex2 = null;

			InputStream io = new FileInputStream(file.getAbsoluteFile());
			Workbook workbook = new XSSFWorkbook(io);
			Sheet sheet = workbook.getSheetAt(0);
			int firstrow = sheet.getFirstRowNum();
			int lastrow = sheet.getLastRowNum();
			// System.out.println("边集：");
			for (int i = firstrow; i < lastrow + 1; i++) {
				Row row = sheet.getRow(i);
				if (row != null) {
					Cell cellA = row.getCell(0);
					Cell cellB = row.getCell(1);
					vex1 = cellA.toString();
					vex2 = cellB.toString();
					// System.out.println(vex1 + "---" + vex2);// 输出边集
					for (Map.Entry<String, Integer> entry : vexsMap.entrySet()) {
						if (entry.getKey().equals(vex1)) { // 在点集中查找输入的第一个顶点的位置
							vex1Site = entry.getValue();
							break;
						}
					}
					for (Map.Entry<String, Integer> entry : vexsMap.entrySet()) {
						if (entry.getKey().equals(vex2)) { // 在点集中查找输入的第二个顶点的位置
							vex2Site = entry.getValue();
							break;
						}
					}
					if (this.arcs[vex1Site][vex2Site] == 0) { // 检测该边是否已经输入
						this.arcs[vex1Site][vex2Site] = 1; // 1表示该位置所对应的两顶点之间有边
						this.arcs[vex2Site][vex1Site] = 1; // 对称边也置1
					}
				}
				io.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			// 加载注释
			InputStream io = new FileInputStream(file.getAbsoluteFile());
			XSSFWorkbook workbook = new XSSFWorkbook(io);
			Sheet sheet = workbook.getSheetAt(2);

			int firstrow = sheet.getFirstRowNum();
			int lastrow = sheet.getLastRowNum();
			String name = sheet.getRow(0).getCell(0).toString();
			List<String> noteList = new ArrayList<String>();// value
			for (int i = firstrow; i < lastrow + 1; i++) {
				Row row = sheet.getRow(i);
				Notes.add(row.getCell(1).toString());
				if (!name.equals(row.getCell(0).toString())) {
					name = row.getCell(0).toString();
					noteList = new ArrayList<String>();
				}
				noteList.add(row.getCell(1).toString());
				nodeNotes.put(name, noteList);
				io.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			// 加载CYC
			InputStream io = new FileInputStream(file.getAbsoluteFile());
			XSSFWorkbook workbook = new XSSFWorkbook(io);
			Sheet sheet = workbook.getSheetAt(3);

			int firstrow = sheet.getFirstRowNum();
			int lastrow = sheet.getLastRowNum();

			String complex = sheet.getRow(0).getCell(1).toString();
			List<String> cycList = new ArrayList<String>();

			for (int i = firstrow; i < lastrow + 1; i++) {
				Row row = sheet.getRow(i);
				if (!complex.equals(row.getCell(1).toString())) {
					complex = row.getCell(1).toString();
					CYC.add(cycList);
					cycList = new ArrayList<String>();
				}
				cycList.add(row.getCell(0).toString());
				io.close();
			}
			CYC.add(cycList);
		} catch (Exception e) {
			e.printStackTrace();
		}

//		System.out.println("点集：");
//		for (Map.Entry<String, Integer> entry : vexsMap.entrySet()) {
//			System.out.println("节点：" + entry.getKey() + "  " + "编号： " + entry.getValue());
//		}
//		System.out.println("注释集：");
//		for (Map.Entry<String, List<String>> entry : nodeNotes.entrySet()) {
//			System.out.println("节点：" + entry.getKey() + "  " + "注释： " + entry.getValue());
//		}
		System.out.println("PPI网络读取成功！");
		System.out.println("WPC算法第一阶段完成！");
		System.out.println("----------------------------------------------------------------------------------------------------------------");
	}

	// 一阶邻居节点（度）---------------------------------------------------------------------------------------
	public void AdjacentNodes() {
		int num;
		for (int i = 0; i < vexsMap.size(); i++) {
			num = 0;
			for (int j = 0; j < vexsMap.size(); j++) {
				if (arcs[i][j] == 1) {
					num++;
				}
			}
			AdjacetNode.put(getKey(vexsMap, i), num);
		}
		System.out.println("一阶邻居节点计算完毕。");
		System.out.println("");
		// 打印一阶邻居节点
//		for (Map.Entry<String, Integer> entry : AdjacetNode.entrySet()) {
//			System.out.println("节点：" + entry.getKey() + "  " + "度： " + entry.getValue());
//		}
	}

	// 共同(交集)邻居节点------两节点的邻居节点个数取交集-----------------------------------------------------------------------
	public void CommonNodes() {

		CommonNode = new int[vexsMap.size()][vexsMap.size()];

		for (int i = 0; i < vexsMap.size(); i++) {
			for (int j = 0; j < vexsMap.size(); j++) {

				int n = 0;
				if (i == j) {
					CommonNode[i][j] = 0;
					continue;
				}
				for (int k = 0; k < vexsMap.size(); k++) {
					if ((arcs[i][k] == 1) && (arcs[j][k] == 1)) {
						n++;
						CommonNode[i][j] = n;
					}
				}
			}
		}
		// 打印共同邻居节点
//		for (int i = 0; i < vexsMap.size(); i++) {
//			for (int j = 0; j < vexsMap.size(); j++) {
//				System.out.print(CommonNode[i][j] + " ");
//			}
//			System.out.println();
//		}
		System.out.println("共同(交集)邻居节点计算完毕。");
		System.out.println("");
	}

	// 并集邻居节点------------两节点的邻居节点个数取并集---------------------------------------------------------------
	public void UnionNodes() {

		UnionNode = new int[vexsMap.size()][vexsMap.size()];
		for (int i = 0; i < vexsMap.size(); i++) {
			for (int j = 0; j < vexsMap.size(); j++) {
				if (i == j) {
					continue;
				}
				UnionNode[i][j] = AdjacetNode.get(getKey(vexsMap, i)) + AdjacetNode.get(getKey(vexsMap, j))
						- CommonNode[i][j];
			}
		}
		// 打印并集邻居节点
//		for (int i = 0; i < vexsMap.size(); i++) {
//			for (int j = 0; j < vexsMap.size(); j++) {
//				System.out.print(UnionNode[i][j] + " ");
//			}
//			System.out.println();
//		}
		System.out.println("并集邻居节点计算完毕。");
		System.out.println("");
	}

	// 拓扑结构相似性------------------------------------------------------------------------------------------
	public void T_sim() {

		t_sims = new float[vexsMap.size()][vexsMap.size()];
		for (int i = 0; i < vexsMap.size(); i++) {
			for (int j = 0; j < vexsMap.size(); j++) {
				if (i == j) {
					continue;
				} // 分子=|N(A)∩N(B)|=共同邻居节点 分母=|N(A)UN(B)|=并集邻居节点
				t_sims[i][j] = (float) CommonNode[i][j] / (float) UnionNode[i][j];
			}
		}
//		for (int i = 0; i < vexsMap.size(); i++) {
//			for (int j = 0; j < vexsMap.size(); j++) {
//				System.out.printf("%-8.4f", t_sims[i][j]);
//			}
//			System.out.println();
//		}
		System.out.println("拓扑结构相似T_sim计算完毕。");
		System.out.println("");
	}

// 语义相似性------------------------------------------------------------------------------------------
	public void S_sim() {
		int Max = 0;
		int MaxSite = 0;
		float fenmu = 0;
		float fenzi = 0;
		s_sims = new float[vexsMap.size()][vexsMap.size()];

		for (int i = 0; i < vexsMap.size(); i++) {// 找到所有基因中包含注释信息量最大的基因
			if (nodeNotes.get(getKey(vexsMap, i)).size() > Max) {
				Max = nodeNotes.get(getKey(vexsMap, i)).size();
				MaxSite = i;
			}
		}
		for (String string : Notes) {// 得到每条注释在注释库中出现的次数 P(t)
			if (Note.containsKey(string)) {
				Note.put(string, Note.get(string).intValue() + 1);
			} else {
				Note.put(string, new Integer(1));
			}
		}

		// 得到分母
		List<String> list = nodeNotes.get(getKey(vexsMap, MaxSite));
		for (int i = 0; i < list.size(); i++) {
			fenmu += IC(list.get(i));
		}

		// 得到分子并计算s_sims
		List<String> listA = new ArrayList<String>();
		List<String> listB = new ArrayList<String>();
		List<String> listTmp = new ArrayList<String>();
		for (int i = 0; i < vexsMap.size(); i++) {
			for (int j = 0; j < vexsMap.size(); j++) {
				if (i == j) {
					continue;
				}
				fenzi = 0;
				listA = nodeNotes.get(getKey(vexsMap, i));
				listB = nodeNotes.get(getKey(vexsMap, j));
				listTmp = (List<String>) CollectionUtils.intersection(listA, listB);// 取交集
				// System.out.println(listTmp);

				for (int k = 0; k < listTmp.size(); k++) {
					fenzi += IC(listTmp.get(k));
					s_sims[i][j] = fenzi / fenmu;
				}
			}
		}
//		for (int i = 0; i < vexsMap.size(); i++) {
//			for (int j = 0; j < vexsMap.size(); j++) {
//				System.out.printf("%-8.4f", s_sims[i][j]);
//			}
//			System.out.println();
//		}
		System.out.println("拓扑语义相似性S_sim计算完毕。");
		System.out.println("");
	}

// 权值------------------------------------------------------------------------------------------
	public void weight() {
		weights = new float[vexsMap.size()][vexsMap.size()];

		for (int i = 0; i < vexsMap.size(); i++) {
			for (int j = 0; j < vexsMap.size(); j++) {
				weights[i][j] = 0;
			}
		}

		for (int i = 0; i < vexsMap.size(); i++) {
			for (int j = 0; j < vexsMap.size(); j++) {
				weights[i][j] = (s_sims[i][j] + t_sims[i][j]) / 2;
			}
		}
//		for (int i = 0; i < vexsMap.size(); i++) {
//			for (int j = 0; j < vexsMap.size(); j++) {
//				System.out.printf("%-8.4f", weights[i][j]);
//			}
//			System.out.println();
//		}
		System.out.println("权值计算完毕。");
		System.out.println("WPC算法第二阶段完成！");
		System.out.println("----------------------------------------------------------------------------------------------------------------");
	}

// WPC_1------------------------------------------------------------------------------------------------------------
	public void WPC_1() {
		float num = 0;
		int n = 0;
		List<String> CS = new ArrayList<String>();
		for (Map.Entry<String, Integer> entry : vexsMap.entrySet()) {
			CS.clear();
			CS.add(entry.getKey());
			for (int i = 0; i < vexsMap.size(); i++) {
				if (arcs[entry.getValue()][i] == 1) {
					CS.add(getKey(vexsMap, i));
				}
			}
			if (AWD(entry.getKey(), CS) != 0) {
				num += AWD(entry.getKey(), CS);
				n++;
				Matrix.put(entry.getKey(), AWD(entry.getKey(), CS));
				Th = num / n;
			}
		}

//		for (Map.Entry<String, Float> entry : Matrix.entrySet()) {
//			System.out.println("节点：" + entry.getKey() + "  " + "AWD： " + entry.getValue());
//		}
		System.out.print("Th:");
		System.out.printf("%-8.4f", Th);
		System.out.println();
		System.out.print("Seeds:");
		Sort(Matrix);
		for (int i = 0; i < Seeds.size(); i++) {
			System.out.print(Seeds.get(i) + " ");
		}
		System.out.println("");
		System.out.println("WPC_1计算完毕。");
		System.out.println("");
	}

// WPC_2------------------------------------------------------------------------------------------------------------
	public void WPC_2() {
		Map<String, Integer> Rejected = new HashMap<String, Integer>();// 节点标记 标记为 1的点丧失成为种子节点权力
		// List<String> CS = new ArrayList<String>();
		List<String> Remove = new ArrayList<String>();

		for (int i = 0; i < Seeds.size(); i++) {// 初始化节点标记
			Rejected.put(Seeds.get(i), 0);
		}

		for (int i = 0; i < Seeds.size(); i++) {
			if (Rejected.get(Seeds.get(i)) == 0) {
				List<String> CS = new ArrayList<String>();
				CS.add(Seeds.get(i));
				for (int j = 0; j < Seeds.size(); j++) {// 一阶节点入集合
					if (arcs[vexsMap.get(Seeds.get(i))][vexsMap.get(Seeds.get(j))] == 1) {
						CS.add(Seeds.get(j));
					}
				}
				for (int j = CS.size() - 1; j >= 0; j--) {
					if (WR(CS.get(j), CS) < Thres_WR) {
						if (!Remove.contains(CS.get(j))) {
							Remove.add(CS.get(j));
						}
						Rejected.put(CS.get(j), 1);
						CS.remove(j);
					}
				}
				if (CS.size() > 1) {
					PC.add(CS);
				}
			}
		}
		for (int i = 0; i < Remove.size(); i++) {
			for (int j = 0; j < PC.size(); j++) {
				if ((AWD(Remove.get(i), PC.get(j)) > Th) && (!PC.get(j).contains(Remove.get(i)))) {
					PC.get(j).add(Remove.get(i));
				}
			}
		}
		System.out.print("算法识别得到PC:");
		System.out.println(PC);
		System.out.println("WPC_2计算完毕。");
		System.out.println("");
	}

// WPC_3重叠处理------------------------------------------------------------------------------------------------------------
	public void WPC_3() {
		List<List<String>> Remove = new ArrayList<List<String>>();
		for (int i = 0; i < PC.size(); i++) {
			for (int j = i + 1; j < PC.size(); j++) {
				if (NA(PC.get(i), PC.get(j)) > NA_WR) {
					if (WDensity(PC.get(i)) > WDensity(PC.get(j))) {
						if (!Remove.contains(PC.get(j))) {
							Remove.add(PC.get(j));
						}
					} else {
						if (!Remove.contains(PC.get(i))) {
							Remove.add(PC.get(i));
						}
					}
				}
			}
		}
		Iterator<List<String>> iterator = PC.iterator();
        while (iterator.hasNext()){
        	List<String> obj = iterator.next();
        	for (int i = 0; i < Remove.size(); i++) {
        		if(obj == Remove.get(i)){
                    iterator.remove();
                }
			}
            
        }
		System.out.print("重叠处理PC:");
		System.out.println(PC);
		System.out.println("WPC计算完毕。");
		System.out.println("WPC算法第三阶段完成！");
		System.out.println("----------------------------------------------------------------------------------------------------------------");
	}

// WPC_CYC实验结果与分析------------------------------------------------------------------------------------------------------------
	public void WPC_CYC() {
		List<List<String>> TNtemp = new ArrayList<List<String>>();
		int TN = 0;
		int TP = 0;
		int tag = 0;
		for (int i = 0; i < PC.size(); i++) {
			tag = 0;
			for (int j = 0; j < CYC.size(); j++) {
				if (NA(PC.get(i), CYC.get(j)) >= 0.2) {
					if (!TNtemp.contains(CYC.get(j))) {
						TNtemp.add(CYC.get(j));
					}
					tag = 1;
				}
			}
			if (tag == 1) {
				TP++;
			}
		}
		TN = TNtemp.size();

		System.out.print("成功读取CYC:");
		System.out.println(CYC);
		System.out.println("将PC与CYC进行匹配，计算Recall、Precision、F-measure值。");
		
		System.out.println("TP:" + TP);
		System.out.println("TN:" + TN);
		Recall = (float)TN / CYC.size();
		Precision = (float)TP / PC.size();
		F_measure = (2 * Recall * Precision) / (Recall + Precision);
		F_measure = (float) (Math.floor(F_measure * 1000) / 1000);
		System.out.println("Recall:" + Recall);
		System.out.println("Precision:" + Precision);
		System.out.println("F_measure:" + F_measure);
		System.out.println("WPC计算完毕。");
		System.out.println("WPC算法第四阶段完成！");
		System.out.println("----------------------------------------------------------------------------------------------------------------");
	}

// NA(A,B)------------------------------------------------------------------------------------------------------------
	public float NA(List<String> A, List<String> B) {
		float overlapRate = 0;// 重叠率
		List<String> listTemp = new ArrayList<String>();

		listTemp = (List<String>) CollectionUtils.intersection(A, B);// 取交集
		overlapRate = ((float) listTemp.size() * (float) listTemp.size()) / ((float) A.size() * (float) B.size());
		return overlapRate;
	}

// WDensity(G)------------------------------------------------------------------------------------------------------------
	public float WDensity(List<String> G) {
		float weightDensity = 0;// 加权稠密度
		float weight = 0;
		for (int i = 0; i < G.size(); i++)
			for (int j = i + 1; j < G.size(); j++) {
				if (arcs[vexsMap.get(G.get(i))][vexsMap.get(G.get(j))] == 1) {
					weight += weights[vexsMap.get(G.get(i))][vexsMap.get(G.get(j))];
				}
			}
		weightDensity = (2 * weight) / (float) (G.size() * (G.size() - 1));
		weightDensity = (float) (Math.round(weightDensity * 10000)) / 10000;
		return weightDensity;
	}

// WD(node,G)------------------------------------------------------------------------------------------------------------
	public float WD(String node, List<String> G) {
		float WD = 0;// 总权值
		for (int i = 1; i < G.size(); i++) {
			WD += weights[vexsMap.get(node)][vexsMap.get(G.get(i))];
		}
		WD = (float) (Math.round(WD * 10000)) / 10000;
		return WD;
	}

// WR(node,G)------------------------------------------------------------------------------------------------------------
	public float WR(String node, List<String> G) {
		float WR = 0;
		List<String> Gn = new ArrayList<String>();
		List<String> GnTemp = new ArrayList<String>();
		Gn.add(node);
		for (int i = 0; i < Seeds.size(); i++) {// 一阶节点入集合
			if (arcs[vexsMap.get(node)][vexsMap.get(Seeds.get(i))] == 1) {
				GnTemp.add(Seeds.get(i));
			}
		}

		for (int i = 0; i < GnTemp.size(); i++) {// 二阶节点入集合
			for (int j = 0; j < Seeds.size(); j++) {
				if ((arcs[vexsMap.get(GnTemp.get(i))][vexsMap.get(Seeds.get(j))] == 1)
						&& (!Gn.contains(Seeds.get(j)))) {
					Gn.add(Seeds.get(j));
				}
			}
		}

		WR = WD(node, G) / (WD(node, G) + WD(node, Gn));
		WR = (float) (Math.round(WR * 10000)) / 10000;
		return WR;
	}

// getKey()------------------------------------------------------------------------------------------------------------
	public String getKey(Map<String, Integer> map, Integer value) {
		String key = "";
		for (Map.Entry<String, Integer> entry : map.entrySet()) {
			if (entry.getValue().equals(value)) {
				key = entry.getKey();
			}
		}
		return key;
	}

// IC(t)=-log(p(t))-------------------------------------------------------------------------------------------------------
	public float IC(String t) {
		float IC = (float) -Math.log(Note.get(t));
		return IC;
	}

// Sort()------------------------------------------------------------------------------------------------------------------
	public void Sort(Map<String, Float> Matrix) {
		float temp = 0;
		List<Float> seedValue = new ArrayList<Float>();
		List<String> seedTemp1 = new ArrayList<String>();
		Map<String, Integer> seedTemp2 = new HashMap<String, Integer>();

		for (Map.Entry<String, Float> entry : Matrix.entrySet()) {
			seedValue.add(entry.getValue());
		}
		// Collections.sort(seedValue);//升序
		Collections.sort(seedValue, Collections.reverseOrder());// 降序
		for (int i = 0; i < seedValue.size(); i++) {
			seedTemp1.clear();
			for (Map.Entry<String, Float> entry : Matrix.entrySet()) {
				if (seedValue.get(i).equals(entry.getValue())) {
					seedTemp1.add(entry.getKey());
				}
			}
			if (seedTemp1.size() == 1) {
				Seeds.addAll(seedTemp1);
			}
			if (seedTemp1.size() > 1) {
				for (int j = 0; j < seedTemp1.size(); j++) {
					seedTemp2.put(seedTemp1.get(j), AdjacetNode.get(seedTemp1.get(j)));
				}
				seedTemp2 = sortByValueDescending(seedTemp2);
				for (Map.Entry<String, Integer> entry : seedTemp2.entrySet()) {
					Seeds.add(entry.getKey());
				}
			}
		}
	}

// 降序排序------------------------------------------------------------------------------------------------------------------
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValueDescending(Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			@Override
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				int compare = (o1.getValue()).compareTo(o2.getValue());
				return -compare;
			}
		});

		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

// AWD()------------------------------------------------------------------------------------------------------------
	public float AWD(String node, List<String> CS) {
		float wNumer = 0;// 总权值
		float AWD = 0;// 平均加权度

		for (int i = 0; i < CS.size(); i++) {
			wNumer += weights[vexsMap.get(node)][vexsMap.get(CS.get(i))];
		}
		AWD = wNumer / (float) (CS.size() - 1);
		AWD = (float) (Math.round(AWD * 10000)) / 10000;
		return AWD;
	}
}
