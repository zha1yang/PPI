package AMGraphTest;

import java.io.File;
import java.util.Scanner;

public class Main {

	public static void main(String[] args) {
		int vexNum = 0;
		Scanner sc =  new Scanner(System.in);
		System.out.print("������Ҫ��������ͼ���ܶ�����(2279)��");
		vexNum = sc.nextInt();
		File file = new File("E:\\test.xlsx");
        AMGraph aMGraph = new AMGraph(vexNum,file);
        
        System.out.println("һ���ھӽڵ㣺");
        aMGraph.AdjacentNodes();
        System.out.println("��ͬ�ھӽڵ㣺");
        aMGraph.CommonNodes();
        System.out.println("�����ھӽڵ㣺");
        aMGraph.UnionNodes();
        System.out.println("���˽ṹ������T_sim��");
        aMGraph.T_sim();
        System.out.println("����������S_sim��");
        aMGraph.S_sim();
        System.out.println("Ȩֵ��");
        aMGraph.weight();
        System.out.println("WPC_1��");
        aMGraph.WPC_1();
        System.out.println("WPC_2��");
        aMGraph.WPC_2();
        System.out.println("WPC_3��");
        aMGraph.WPC_3();
        System.out.println("WPC_CYC��");
        aMGraph.WPC_CYC();
    }

}
