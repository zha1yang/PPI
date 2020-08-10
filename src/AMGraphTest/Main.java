package AMGraphTest;

import java.io.File;
import java.util.Scanner;

public class Main {

	public static void main(String[] args) {
		int vexNum = 0;
		Scanner sc =  new Scanner(System.in);
		System.out.print("请输入要建立无向图的总顶点数(2279)：");
		vexNum = sc.nextInt();
		File file = new File("E:\\test.xlsx");
        AMGraph aMGraph = new AMGraph(vexNum,file);
        
        System.out.println("一阶邻居节点：");
        aMGraph.AdjacentNodes();
        System.out.println("共同邻居节点：");
        aMGraph.CommonNodes();
        System.out.println("并集邻居节点：");
        aMGraph.UnionNodes();
        System.out.println("拓扑结构相似性T_sim：");
        aMGraph.T_sim();
        System.out.println("语义相似性S_sim：");
        aMGraph.S_sim();
        System.out.println("权值：");
        aMGraph.weight();
        System.out.println("WPC_1：");
        aMGraph.WPC_1();
        System.out.println("WPC_2：");
        aMGraph.WPC_2();
        System.out.println("WPC_3：");
        aMGraph.WPC_3();
        System.out.println("WPC_CYC：");
        aMGraph.WPC_CYC();
    }

}
