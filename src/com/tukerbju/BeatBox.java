package com.tukerbju;
import java.awt.*;
import java.io.*;
import javax.swing.*;
import javax.sound.midi.*;
import java.util.*;
import java.awt.event.*;
import java.net.*;
import javax.swing.event.*;

public class BeatBox 
{ //components and variables visible by listeners
	JFrame theFrame; 
	JPanel mainPanel;
	JList incomingList;
	JTextField userMessage;
	ArrayList<JCheckBox> checkboxList;
	int nextNum=1;
	Vector<String> listVector = new Vector<String>();
	String userName;
	ObjectOutputStream out;
	ObjectInputStream in;
	HashMap<String,boolean[]> otherSeqsMap = new HashMap<String, boolean[]>();
	
	Sequencer sequencer;
	Sequence sequence;
	Sequence mySequence = null;
	Track track;
	
	
	String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare",
	"Crash Cymbal", "Hand Clap", "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga", "Cowbell",
	"Vibraslap", "Low-mid Tom", "High Agogo", "Open Hi Conga"}; //array with drum names 
	
	int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};//numbers for different drums
	//we use drums as notes in other instruments
	
	public static void main(String[] args)
	{
		try
		{
			new BeatBox().startUp(args[0]);
		} catch (ArrayIndexOutOfBoundsException ex)
		{
			char[] randomLetters = {(char) (64+Math.random()*26), (char) (64+Math.random()*26), (char) (64+Math.random()*26)};
			String defaultName = new String(randomLetters);
			new BeatBox().startUp(defaultName);
		}
	}
	
	public void startUp (String name)
	{ // in method sartUp at first we connect to the server
		userName = name;
		try
		{
			Socket sock = new Socket("127.0.0.1", 4244); // connects to the server - localhost on TCP-port 4244 
			out = new ObjectOutputStream(sock.getOutputStream()); //creates new output stream to the server to send objects 
			in = new ObjectInputStream(sock.getInputStream()); //creates new input stream to the server to send objects 
			Thread remote = new Thread(new RemoteReader());
			//creates new thread for receiving beat combination from the server
			remote.start(); // starts new thread for receiving beat combination from the server
		} 
		catch (Exception ex)
		{
			System.out.println("couldn't connect - you'll have to play alone");
		}
		setUpMidi();
		buildGUI();
	}
	
	public void buildGUI()
	{
		theFrame = new JFrame("Cyber BeatBox"); //initialize frame with inscription CyberBeatBox
		//theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		BorderLayout layout = new BorderLayout();  
		JPanel background = new JPanel(layout); //creates new JPanel background with Border layout manager 
		background.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		// creates 10-pixel wide empty border of panel
		checkboxList = new ArrayList<JCheckBox>(); //creates an ArrayList for checkboxes
		
		Box buttonBox = new Box (BoxLayout.Y_AXIS); //creates a box for buttons
		JButton start = new JButton("Start");
		start.addActionListener((e) -> buildTrackAndStart()); //composes a track and plays it
		
		buttonBox.add(start); //creates start button, adds an ActionListener and adds button to buttonBox
		
		JButton stop = new JButton("Stop");
		stop.addActionListener((e) -> sequencer.stop());
		buttonBox.add(stop);//creates stop button, adds an ActionListener and adds button to buttonBox
		
		JButton upTempo = new JButton("Tempo Up");
		upTempo.addActionListener((e) -> 
		{
			float tempoFactor = sequencer.getTempoFactor(); //increases tempo with method setTempoFactor()
			sequencer.setTempoFactor((float) (tempoFactor*1.03));
		});
		buttonBox.add(upTempo);//creates upStart button, adds an ActionListener and adds button to buttonBox
		
		JButton downTempo = new JButton("Tempo Down");
		downTempo.addActionListener( (e) -> 
		{
			float tempoFactor = sequencer.getTempoFactor(); //decreases tempo with method setTempoFactor()
			sequencer.setTempoFactor((float) (tempoFactor*0.97));
		});
		buttonBox.add(downTempo);//creates downTempo button, adds an ActionListener and adds button to buttonBox
		
		JButton sendIt = new JButton("sendIt");
		sendIt.addActionListener(new MySendListener());
		buttonBox.add(sendIt);//creates sendIt button, adds an ActionListener and adds button to buttonBox
		
		JButton randomButton = new JButton("randomSeq");
		randomButton.addActionListener(new MyRandomListener());
		buttonBox.add(randomButton); //creates random beat sequence
		
		JButton saveButton = new JButton("Save");
		saveButton.addActionListener(new MySaveListener());
		buttonBox.add(saveButton); //saves sequence in the .ser file
		
		JButton openButton = new JButton("Open");
		openButton.addActionListener(new MyOpenListener());
		buttonBox.add(openButton);
		
		
		userMessage = new JTextField(); //creates text field for user messages
		buttonBox.add(userMessage); // adds userMessage JTextField to buttonBox
		
		incomingList = new JList(); //creates new incomingList JList
		incomingList.addListSelectionListener(new MyListSelectionListener());
		// adds ListSelectionListener to incomingList JList
		incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		// selects single selection mode for incomingList JList
		JScrollPane theList = new JScrollPane(incomingList); 
		// adds incomingList to the new theList JScrollPane
		buttonBox.add(theList); //adds theList to buttonBox
		incomingList.setListData(listVector); //put data from listVector to incomingList
		
		Box nameBox = new Box(BoxLayout.Y_AXIS);//creates a box for names
		for (int i = 0; i < 16; i++)
		{
			nameBox.add(new Label(instrumentNames[i])); //adds names to the box from the array instrumentNames 
		}
		
		background.add(BorderLayout.EAST, buttonBox); //buttonBox to EAST area
		background.add(BorderLayout.WEST, nameBox); //namesBox to WEST area
		
		theFrame.getContentPane().add(background); //adds background panel to the frame
		GridLayout grid = new GridLayout(16,16); 
		grid.setVgap(1);  
		grid.setHgap(2);
		mainPanel = new JPanel(grid); //creates new mainPanel with layout GridLayout layout manager
		background.add(BorderLayout.CENTER,mainPanel); //adds mainPanel to background panel
		
		for (int i=0; i<256; i++) //adds check boxes to the mainPanel
		{
			JCheckBox c = new JCheckBox(); 
			c.setSelected(false);
			checkboxList.add(c);
			mainPanel.add(c);
		}
		
		theFrame.setBounds(50,50,300,300); //set bounds of the frame
		theFrame.pack(); //method pack to guarantee that all components will be visible
		theFrame.setVisible(true); // shows the frame
				
	}
	
	public void setUpMidi()
	{
		try
		{
			sequencer = MidiSystem.getSequencer();
			sequencer.open(); //initializes and opens sequencer
			sequence = new Sequence(Sequence.PPQ,4); //initialize sequence
			track = sequence.createTrack(); //creates a track in the sequence
			sequencer.setTempoInBPM(120); // set tempo in bites per minute
		}
		
		catch (Exception e) {e.printStackTrace();}
	}
	
	public void buildTrackAndStart()
	{
		ArrayList <Integer> trackList = null;
		
		
		sequence.deleteTrack(track); //deletes all track
		track = sequence.createTrack(); //creates new track
		
		for (int i=0; i < 16; i++)
		//in this cycle we check all 16 instruments
		{
			trackList = new ArrayList<Integer>();
			
			for (int j=0; j < 16; j++)
			//in this cycle we check all 16 bites for 1 instrument
			{
				JCheckBox jc = (JCheckBox) checkboxList.get(j+(16*i));
				if (jc.isSelected())
				{
					int key = instruments [i]; 
	                 // if checkbox is selected we write note(drum) value to trackList list
					trackList.add(new Integer(key));
				}
				else
				{
					trackList.add(null);//if checkbox not selected we add null to trackList
				}
			}
			
			makeTracks(trackList); // make MIDI-events basing on the information from trackList and adding them to the track; 
			//track.add(makeEvent(176,1,127,0,16));// adds ControllerEvent to 17th bite
		}
		
		track.add(makeEvent(192,9,1,0,15)); //guarantee an event on 16th bite
		try
		{
			sequencer.setSequence(sequence);//set sequence to the sequencer
			sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY); //set endless loop for the track
			sequencer.start(); //start playing
			sequencer.setTempoInBPM(120); //set tempo
		}
		catch (Exception e) {e.printStackTrace();}
	}
	
	
	public class MySendListener implements ActionListener
	{
		public void actionPerformed(ActionEvent ev)
		{
			boolean[] checkboxState = new boolean[256];
			
			for (int i =0; i < 256; i++) 
			//we check every JCheckBox in checkboxList and write results
			// to the array checkboxState
			{
				JCheckBox check = (JCheckBox) checkboxList.get(i);
				if (check.isSelected())
				{
					checkboxState[i] = true;
				}
			}
			String messageToSend = null;
			try
			{
				out.writeObject(userName + nextNum++ + ": "+userMessage.getText());
				out.writeObject(checkboxState);
	//we send to the server two objects 
	//first object is a String consisted of userName(received from command line)
	//nextNum number(number of current message) and text from userMessage JTextField
				System.out.println("sending");
			}
			catch (Exception ex)
			{
				System.out.println("Sorry dude, couldn't send it to the server");
			}
			userMessage.setText("");
			
		
		}
	}
	public class MyRandomListener implements ActionListener
	{
		public void actionPerformed(ActionEvent ev)
		{
			int randomNum;
			boolean[] randomSequence = new boolean[256];
			for (int x=0; x<randomSequence.length; x++)
			{
				randomNum=(int) (Math.random()*100);
				if (randomNum<25)
				{
					randomSequence[x]=true;
				}
				else
				{
					randomSequence[x]=false;
				}
			}
			changeSequence(randomSequence);
			sequencer.stop();
			buildTrackAndStart();
		}
	}
	
	public class MySaveListener implements ActionListener
	{
		public void actionPerformed(ActionEvent ev)
		{
			JFileChooser saveSequence = new JFileChooser();
			saveSequence.showSaveDialog(theFrame);
			File saveFile = saveSequence.getSelectedFile();
			boolean[] sequenceToSave = new boolean[256];
			JCheckBox tempBox;
			for (int x=0; x<sequenceToSave.length; x++)
			{
				tempBox = (JCheckBox) checkboxList.get(x);
				if (tempBox.isSelected())
				{sequenceToSave[x]=true;}
				else
				{sequenceToSave[x]=false;}
					
			}
			try
			{
				ObjectOutputStream oos=new ObjectOutputStream(new FileOutputStream(saveFile));
				oos.writeObject(sequenceToSave);
				oos.close();
			}
			catch (IOException ex)
			{ ex.printStackTrace();}
				
			
		}
	}
	public class MyOpenListener implements ActionListener 
	{
		public void actionPerformed(ActionEvent a)
		{
			boolean[] sequenceFromFile = new boolean[256];
			JFileChooser openSequence = new JFileChooser();
			openSequence.showOpenDialog(theFrame);
			File openFile = openSequence.getSelectedFile();
			try
			{
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(openFile));
				sequenceFromFile = (boolean[]) ois.readObject();
				ois.close();
			} catch (Exception ex)
			{ex.printStackTrace();}
			
			changeSequence(sequenceFromFile);
			sequencer.stop();
			buildTrackAndStart();
			
		}
	}
	public class MyListSelectionListener implements ListSelectionListener
	{
		public void valueChanged(ListSelectionEvent le)
		{
			if (!le.getValueIsAdjusting())
			{
				String selected = (String) incomingList.getSelectedValue();
				
				if (selected!= null)
				{
					// if track from incomingList JList is selected
					//we take boolean array from otherSeqsMap,
					//change Sequence for Sequencer according to array and build track again 
					boolean[] selectedState = (boolean[]) otherSeqsMap.get(selected);
					changeSequence(selectedState); 
					sequencer.stop();
					buildTrackAndStart();
				}
			}
		}
	}
	
	public class RemoteReader implements Runnable
	{
		//class RemoteReader is used as a task for separate thread
		// which should read objects sent by the server
		boolean[] checkboxState = null;
		String nameToShow = null;
		Object obj = null;
		
		public void run()
		{
			try 
			{
				while ((obj=in.readObject()) != null)
				{
					System.out.println("got an object");
					System.out.println(obj.getClass());
					String nameToShow = (String) obj;
					//first read object is a String 
					checkboxState = (boolean[]) in.readObject();
					//second read object is an array of boolean
					otherSeqsMap.put(nameToShow, checkboxState);
					//we immediately add both objects to otherSeqsMap 
					//String as a key, boolean array as a value
					listVector.add(nameToShow);
					//we add received String to listVector
					incomingList.setListData(listVector);
					//we fill incomingList field with data from listVector
					
				}
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
			
		}
	}
	
	public void changeSequence(boolean[] checkboxState)
	{
		for (int i=0; i<256; i++)
		{
			JCheckBox check = (JCheckBox) checkboxList.get(i);
			if (checkboxState[i])
			{
				check.setSelected(true);
			}
			else
			{
				check.setSelected(false);
			}
		}
	}
	
	public void makeTracks (ArrayList list)
	{
		Iterator it = list.iterator();
		for (int i = 0; i<16; i++)
		{
			Integer num = (Integer) it.next();
			if (num != null)
			{
				int numKey = num.intValue();
				track.add(makeEvent(144,9,numKey,100,i));
				track.add(makeEvent(128,9,numKey, 100, i+1));
			}
		}
	}
	
	public static MidiEvent makeEvent(int comd, int chan, int one, int two, int tick)
	{ //this method helps us to shorten event adding to the track.
	  // now we need one method makeEvent() instead all this code;
		
		MidiEvent event = null;
		try
		{
			ShortMessage message=new ShortMessage();
			message.setMessage(comd,chan,one,two);
			event=new MidiEvent(message,tick);
		}
		catch (Exception e)
		{}
		return event;
	}

}
