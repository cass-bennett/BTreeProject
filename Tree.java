import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.AbstractMap.SimpleEntry;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Stack;

public class Tree implements Iterable<SimpleImmutableEntry<String,Integer>> {
    // public static final String DIRECTORY = "trees.d";
    public final String FILEPATH ;
    public static final int ORDER = Node.ORDER;
    public static final int MAX_KEYS = Node.MAX_KEYS;
    public static final int NODE_SIZE = Node.NODE_SIZE;
    public Node root;
    private int numNodes;
    
    public Tree(String fPath) {
	numNodes = 1;
	root = new Node( 0 );
	FILEPATH = fPath;
	// FILEPATH = DIRECTORY + "/" + fileName;
	//	this.readFromFile();
    }

    public int getNumNodes() {
	return numNodes;
    }

    public Tree add( String str ) {
	if( ! Node.canAdd( str ) ) {
	    System.err.println("Error: Could not add \"" + str +
			       "\" to tree because it's too big.");
	} else {
	    Stack<Node> nodes = new Stack<Node>();
	    Node n = root;
	    nodes.push(n);
	    while( ! n.isLeaf() && ! n.contains( str ) ) {
		n = n.getLink( findInsertionPoint( str, n ) );
		nodes.push(n);
	    }
	    try {
		RandomAccessFile file = new RandomAccessFile(FILEPATH, "rw");
		add( str, nodes, file );
		file.close();
	    } catch ( IOException e ) {
		e.printStackTrace();
	    }
	}
	return this;
    }
    /**
     * @param addStr the word that's being added to the node
     * @param nodes  a stack containing the node being added to and all its
     * parents
     * @param file   the file on which the tree is saved
     */
    private Tree add( String addStr, Stack<Node> nodes, RandomAccessFile file )
	throws IOException {
	Node addNode = nodes.pop();
	int addFreq = 1;
	int index;
	boolean keepAdding = true;
	
	// variables to keep track of node splitting
	Node parent, left = null, right = null;

	while(keepAdding) {
	    keepAdding = false;
	    index = findInsertionPoint( addStr, addNode );
	    // If the word to be added is already in the node then just
	    // update the frequency value
	    if( index < MAX_KEYS && addStr.equals( addNode.getWord(index) )) {
		addNode.setFreq( index , addNode.getFreq( index ) + addFreq );
		addNode.updateOnFile( index, file );
	    }
	    // If the node isn't already full then add the word to an empty slot
	    else if( ! addNode.isFull() ) {
		addNode.shiftRight( index );
		addNode.set( index , addStr , addFreq );
		if( left != null && right != null ) {
		    addNode.setLink( index , left );
		    addNode.setLink( index + 1 , right );
		}
		addNode.updateOnFile( index, file );
	    }
	    // If the node needs to be split
	    else {
		// Make lists to keep track of the order in which words and
		// frequencies will be added to the left and right children
		LinkedList<String>  addWords = new LinkedList<String>();
		LinkedList<Integer> addFreqs = new LinkedList<Integer>();
		LinkedList<Node>    addLinks = null;
		if( left != null && right != null )
		    addLinks = new LinkedList<Node>();

		// add words, frequencies and links to the lists declared above
		for( int i = 0 ; i < MAX_KEYS ; i ++ ) {
		    if( i == index ) {
			addWords.add( addStr );
			addFreqs.add( 1 );
		    }
		    addWords.add( addNode.getWord(i) );
		    addFreqs.add( addNode.getFreq(i) );
		}
		if( index >= MAX_KEYS ) {
		    addWords.add( addStr );
		    addFreqs.add( 1 );
		}
		if( addLinks != null ) {
		    for( int i = 0 ; i < ORDER ; i ++ ) {
			if( i != index )
			    addLinks.add( addNode.getLink(i) );
			else {
			    addLinks.add( left );
			    addLinks.add( right );
			}
		    }
		}
		// Make new nodes
		if( nodes.empty() ) {
		    parent = new Node( addNode.ADDRESS ) ;
		    root = parent;
		    left   = new Node( numNodes     );
		    right  = new Node( numNodes + 1 );
		    numNodes += 2;
		}
		else {
		    parent = nodes.pop();
		    left   = new Node( addNode.ADDRESS );
		    right  = new Node( numNodes );
		    numNodes += 1;
		}
		
		// Copy over words, frequencies and links to the new children
		for( int i = 0 ; i < (MAX_KEYS / 2) ; i++ ) {
		    left.set( i , addWords.remove() , addFreqs.remove() );
		}
		addStr = addWords.remove();
		addFreq = addFreqs.remove();
		// Copy over words, frequencies and links to the right node
		for( int i = 0 ; i < (MAX_KEYS / 2) ; i++ ) {
		    right.set( i , addWords.remove() , addFreqs.remove() );
		}
		if( addLinks != null ) {
		    for( int i = 0 ; i < (MAX_KEYS) / 2 + 1 ; i ++ )
			left.setLink( i , addLinks.remove() );
		    for( int i = 0 ; i < (MAX_KEYS) / 2 + 1 ; i ++ )
			right.setLink( i , addLinks.remove() );
		}

		// Update the affected nodes on file
		left.writeToFile( file );
		right.writeToFile( file );

		// Set variables for the next add iteration
		addNode = parent;
		keepAdding = true;

	    }
	}
	return this;
    }
    
    public Tree addAll( Iterable<String> c ) {
	Iterator<String> iter = c.iterator();
	while( iter.hasNext() )
	    this.add( iter.next() );
	return this;
    }

    public boolean contains( String str ) {
	Node current = root;
	String word;
	while( current != null )
	    for( int i = 0; i < MAX_KEYS ; i++ ) {
		word = current.getWord(i);
		if( str.equals( word ) )
		    return true;
		else if( str.compareTo( word ) < 0 ) {
		    current = current.getLink( i );
		    break;
		} else if( i == MAX_KEYS-1 || current.getFreq(i+1) == 0 ) {
		    current = current.getLink(i+1);
		    break;
		}
	    }
	return false;
    }

    public int findNumNodes( Node node ) {
	if( node == null )
	    return 0;
	else if( node.isLeaf() )
	    return 1;
	else {
	    int numNodes = 1;
	    for( int i = 0 ; i < ORDER ; i++ )
		numNodes += findNumNodes( node.getLink(i) );
	    return numNodes;
	}
    }

    public void readFromFile() {
	try {
	    RandomAccessFile file = new RandomAccessFile(FILEPATH, "r");
	    root = Node.readFromFile( 0 , file );
	    numNodes = this.findNumNodes(root);
	    file.close();
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }
    
    private int findInsertionPoint( String str, Node node ) {
	int n = 0;
	for( SimpleImmutableEntry<String,Integer> e : node ) {
	    if( str.compareTo(e.getKey()) > 0 )
		n++;
	    else
		break;
	}
	return n;
    }

    public void printTree() {
	printTree( root );
    }
    public void printTree( Node n ) {
	n.printNode();
	for( int i = 0 ; i < ORDER ; i ++ )
	    if( n.getLink(i) != null )
		printTree( n.getLink(i) );
    }

    public Iterator<SimpleImmutableEntry<String,Integer>> iterator() {
	LinkedList<SimpleImmutableEntry<String,Integer>> words
	    = new LinkedList<SimpleImmutableEntry<String,Integer>>();
	Node currentNode = root;
	int  currentPos;
	Stack<SimpleEntry<Node,Integer>> nodes
	    = new Stack<SimpleEntry<Node,Integer>>();

	// Go as far down the leftmost branch of the tree as possible
	nodes.push(new SimpleEntry<Node,Integer>(root,0) );
	while( ! currentNode.isLeaf() ) {
	    currentNode = currentNode.getLink(0);
	    nodes.push(new SimpleEntry<Node,Integer>(currentNode,0) );
	}
		
	while( ! nodes.empty() ) {
	    currentNode = nodes.peek().getKey();
	    currentPos  = nodes.peek().getValue();

	    words.add(new SimpleImmutableEntry<String,Integer>(
			currentNode.getWord(currentPos),
			currentNode.getFreq(currentPos)));
	    currentPos++;
	    nodes.peek().setValue(currentPos);

	    // If this node has nothing more to traverse then pop it off the
	    // stack so we don't come back to it
	    if(currentPos >= MAX_KEYS || currentNode.getWord(currentPos)==null)
		nodes.pop();
	    
	    // Go down the leftmost possible branch, if it exists
	    if( ! currentNode.isLeaf() ) {
		currentNode = currentNode.getLink(currentPos);
		nodes.push(new SimpleEntry<Node,Integer>(currentNode,0) );
		while( ! currentNode.isLeaf() ) {
		    currentNode = currentNode.getLink(0);
		    nodes.push(new SimpleEntry<Node,Integer>(currentNode,0));
		}
	    }
	}
	return words.iterator();
    }
}
