// These tests were done using JUnit 4.12 and hamcrest 1.3
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.File;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class Tests {
    static final String TEST_OUTPUT_DIR = "test-outputs.d";
    
    @BeforeClass
    public static void openDir() {
	File dir = new File( TEST_OUTPUT_DIR );
	if( ! dir.exists() )
	    dir.mkdir();
    }
    @AfterClass
    public static void removeDir() {
	File dir = new File( TEST_OUTPUT_DIR );
	File[] contents = dir.listFiles();
	if (contents != null) {
	    for (File f : contents) {
		f.delete();
	    }
	}
	dir.delete();
    }

    
    boolean testNodeValues( Node n, String[] words, int[] freqs ) {
	if(words.length != freqs.length || words.length > Node.MAX_KEYS)
	    return false;
	for( int i = 0; i < Node.MAX_KEYS ; i++ ){
	    if( i >= freqs.length ) {
	    	if( n.getWord(i) != null || n.getFreq(i) != 0 )
	    	    return false;
	    } else if( n.getWord(i) == null
		       || (! n.getWord(i).equals(words[i]))
		       || n.getFreq(i) != freqs[i] )
		return false;
	}
	return true;
    }
    
    @Test
    public void nodeFileIO() {
	Node n = new Node(0);
	n.set( 0, "Åçhálîëorßkhriåch", 1);
	n.set( 1, "aaa",2);
	n.set( 2, "zzzz",1);
	n.set( 3, "åáâàå",3);
	n.set( 4, "Güten Tag, World!",1);
	n.set( 5, "こんにちは世界！",1);
	n.set( 6,
	    "IWantedToTestForA64ByteWordSoHereIsOneSuchWordThatIAmTestingNow!"
	       ,1);
	
	n.writeToFile( TEST_OUTPUT_DIR + "/nodeFileIO");
	n = null;
	try{
	    RandomAccessFile file =
		new RandomAccessFile(TEST_OUTPUT_DIR + "/nodeFileIO", "rw");
	    n = Node.readFromFile(0,file);
	    file.close();
	} catch( IOException e ) {
	    e.printStackTrace();
	}
	assertTrue( testNodeValues( n, new String[]{"Åçhálîëorßkhriåch",
	   "aaa","zzzz","åáâàå","Güten Tag, World!","こんにちは世界！",
	   "IWantedToTestForA64ByteWordSoHereIsOneSuchWordThatIAmTestingNow!"},
		new int[]{1,2,1,3,1,1,1} ));
    }
        
    @Test
    public void leafSplit(){
	Tree myTree = new Tree(TEST_OUTPUT_DIR + "/leafSplit");
	myTree.addAll( testWords.subList(0,19) );

	assertTrue(testNodeValues(myTree.root,
				  new String[]{"b","d","f"},
				  new int[]{1,1,1} ));
	assertTrue(testNodeValues(myTree.root.getLink(0),
				  new String[]{"a0","a1","a2","a3"},
				  new int[]{1,1,1,1} ));
	assertTrue(testNodeValues(myTree.root.getLink(1),
				  new String[]{"c0","c1","c2","c3"},
				  new int[]{1,1,1,1} ));
	assertTrue(testNodeValues(myTree.root.getLink(3),
				  new String[]{"g0","g1","g2","g3"},
				  new int[]{1,1,1,1} ));
    }

    @Test
    public void nonLeafSplit(){
	Tree t = new Tree(TEST_OUTPUT_DIR + "/nonLeafSplit");
	t.addAll( testWords );

	assertTrue(testNodeValues(t.root,
				  new String[]{"b","d","f","h","j","l","n","p"},
				  new int[]{1,1,1,1,1,1,1,1} ));
	assertTrue(testNodeValues(t.root.getLink(8),
				  new String[]{"q0","q1","q2","q3"},
				  new int[]{1,1,1,1} ));

	t.addAll( Arrays.asList( "a4","a5","a6","a7","a8" ));
	assertTrue(testNodeValues(t.root,
				  new String[]{"h"},
				  new int[]{1} ));
	assertTrue(testNodeValues(t.root.getLink(0),
				  new String[]{"a4","b","d","f"},
				  new int[]{1,1,1,1} ));
	assertTrue(testNodeValues(t.root.getLink(1),
				  new String[]{"j","l","n","p"},
				  new int[]{1,1,1,1} ));

	t = new Tree(TEST_OUTPUT_DIR + "/nonLeafSplit");
	t.addAll(testWords).addAll(Arrays.asList( "q4","q5","q6","q7","q8" ));
	assertTrue(testNodeValues(t.root,
				  new String[]{"j"},
				  new int[]{1} ));
	assertTrue(testNodeValues(t.root.getLink(0),
				  new String[]{"b","d","f","h"},
				  new int[]{1,1,1,1} ));
	assertTrue(testNodeValues(t.root.getLink(1),
				  new String[]{"l","n","p","q4"},
				  new int[]{1,1,1,1} ));

	t = new Tree(TEST_OUTPUT_DIR + "/nonLeafSplit");
	t.addAll(testWords).addAll(Arrays.asList( "i4","i5","i6","i7","i8" ));
	assertTrue(testNodeValues(t.root,
				  new String[]{"i4"},
				  new int[]{1} ));
	assertTrue(testNodeValues(t.root.getLink(0),
				  new String[]{"b","d","f","h"},
				  new int[]{1,1,1,1} ));
	assertTrue(testNodeValues(t.root.getLink(1),
				  new String[]{"j","l","n","p"},
				  new int[]{1,1,1,1} ));	
    }

    @Test
    public void iteratorTest() {	
	Tree t = new Tree(TEST_OUTPUT_DIR + "/iteratorTest");

	List<String> l = new ArrayList<String>();
	l.addAll( testWords );
	l.addAll( moreTestWords );
	t.addAll( l );
	// list of some words that are already in l
	List<String> repeats = Arrays.asList("a2","A2","d","f",
					     "Z3","c3","n","p");
	t.addAll( repeats );
	l.sort(new Comparator<String>(){
		public int compare(String a, String b){
		    return a.compareTo(b);
		}});
	Iterator<String> listIter = l.iterator();
	Iterator<SimpleImmutableEntry<String,Integer>> treeIter = t.iterator();
	while( listIter.hasNext() ) {
	    SimpleImmutableEntry<String,Integer> ent = treeIter.next();
	    String tStr = ent.getKey();
	    int    tFrq = ent.getValue();
	    String lStr = listIter.next();
	    assertTrue( tStr.equals(lStr) );
	    if( repeats.contains(tStr) )
		assertEquals( tFrq, 2 );
	    else
		assertEquals( tFrq, 1 );
	}
    }

    @Test
    public void TreeFileIO() {
	Tree t = new Tree(TEST_OUTPUT_DIR + "/treeFileIO");

	t.addAll( testWords );
	t.addAll( moreTestWords );
	t.addAll(Arrays.asList("a2","A2","d","f","Z3","c3","n","p"));
	Iterator<SimpleImmutableEntry<String,Integer>> iter0 = t.iterator();

	t = new Tree(TEST_OUTPUT_DIR + "/treeFileIO");
	t.readFromFile();
	Iterator<SimpleImmutableEntry<String,Integer>> iter1 = t.iterator();
	
	while( iter0.hasNext() && iter1.hasNext() ) {
	    SimpleImmutableEntry<String,Integer> e0 = iter0.next();
	    SimpleImmutableEntry<String,Integer> e1 = iter1.next();
	    assertEquals( e0.getValue(), e1.getValue() );
	    assertTrue( e0.getKey().equals( e1.getKey() ));
	}
	assertFalse( iter0.hasNext() || iter1.hasNext() );
    }

    List<String> testWords = Arrays.asList(
	// These should make a node of the tree get split into a node with
	// "e"s and another with "g"s by adding an element on the right:
	"e0","e1","e2","e3","f", "g0","g1","g2","g3",
	// These should make the "e" node split by adding to the left, where the
	// old middle element gets added to the left part of the parent node:
	"b", "a3","a2","a1","a0",
	// These should make the "a" node split by adding to the middle, where
	// the old middle gets added to a middle part of the parent node:
	"c0","c1","c2","c3","d",
	// These should fill out the tree until the root node is the fullest
	// it can be before getting split:
	"h","i0","i1","i2","i3","j","k0","k1","k2","k3","l","m0","m1","m2",
	"m3","n","o0","o1","o2","o3","p","q0","q1","q2","q3");


    List<String> moreTestWords = Arrays.asList( "i4","i5","i6","i7","i8","A0","A1","こんにちは世界！","IWantedToTestForA64ByteWordSoHereIsOneSuchWordThatIAmTestingNow!","A2","A3","A4","A5","A6","A7","A8","A9","B0","B1","B2","B3","B4","B5","B6","B7","B8","B9","C0","C1","C2","C3","C4","C5","C6","C7","C8","C9","D0","D1","D2","D3","D4","D5","D6","D7","D8","D9","E0","E1","E2","E3","E4","E5","E6","E7","E8","E9","F0","F1","F2","F3","F4","F5","F6","F7","F8","F9","G0","G1","G2","G3","G4","G5","G6","G7","G8","G9","H0","H1","H2","H3","H4","H5","H6","H7","H8","H9","I0","I1","I2","I3","I4","I5","I6","I7","I8","I9","J0","J1","J2","J3","J4","J5","J6","J7","J8","J9","K0","K1","K2","K3","K4","K5","K6","K7","K8","K9","L0","L1","L2","L3","L4","L5","L6","L7","L8","L9","M0","M1","M2","M3","M4","M5","M6","M7","M8","M9","N0","N1","N2","N3","N4","N5","N6","N7","N8","N9","O0","O1","O2","O3","O4","O5","O6","O7","O8","O9","P0","P1","P2","P3","P4","P5","P6","P7","P8","P9","Q0","Q1","Q2","Q3","Q4","Q5","Q6","Q7","Q8","Q9","R0","R1","R2","R3","R4","R5","R6","R7","R8","R9","S0","S1","S2","S3","S4","S5","S6","S7","S8","S9","T0","T1","T2","T3","T4","T5","T6","T7","T8","T9","U0","U1","U2","U3","U4","U5","U6","U7","U8","U9","V0","V1","V2","V3","V4","V5","V6","V7","V8","V9","W0","W1","W2","W3","W4","W5","W6","W7","W8","W9","X0","X1","X2","X3","X4","X5","X6","X7","X8","X9","Y0","Y1","Y2","Y3","Y4","Y5","Y6","Y7","Y8","Y9","Z0","Z1","Z2","Z3","Z4","Z5","Z6","Z7","Z8","Z9");

}
