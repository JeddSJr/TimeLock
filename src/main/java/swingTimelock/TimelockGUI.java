/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package swingTimelock;
import com.github.lgooddatepicker.components.DatePicker;
import java.awt.CardLayout;
import java.awt.Color;
import javax.swing.JTextField;
import javax.swing.JComponent;
import java.awt.Toolkit;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.InputVerifier;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.DocumentFilter;
/**
 *
 * @author Jedd
 */


public class TimelockGUI extends javax.swing.JFrame {

  public class inputVerifier extends InputVerifier{

    public boolean shouldYieldFocus(JComponent input){
        boolean inputOK = verify(input);
        if (inputOK) {
            JTextField tf = (JTextField) input;
            tf.setBackground(Color.white);
            tf.setForeground(Color.black);
            return true;
        } else {
            Toolkit.getDefaultToolkit().beep();
            JTextField tf = (JTextField) input;
            tf.setBackground(Color.red);
            tf.setForeground(Color.WHITE);
            return false;
        }
    }
    @Override
    public boolean verify(JComponent input) {
      return checkField(input, false);
    }

    private boolean checkField(JComponent input, boolean state){
      if(input == yearsTextField || input == monthsTextField || input == weeksTextField || input == daysTextField || input == hoursTextField || input == minutesTextField){
        boolean wasValid = false;
        JTextField tf = (JTextField) input;
        String tfString= tf.getText();
        if(tfString.trim().matches("[0-9]+") || tfString.trim().isEmpty())
            wasValid = true;

        return wasValid;
      }
      else{return true;}
    }
  }

  private File file;
  private String filePath;
  private CardLayout cardManager;
  private Boolean activity = false;
  private LocalDate datePickerValue;
  private LocalDateTime lockDuration;
  private JComponent lockComponents[];
  private ArrayList<timedLock> activeLocksList;
  private static final long serialVersionUID = 532110320;
  private Integer minutesFieldValue,hoursFieldValue,daysFieldValue,weeksFieldValue,monthsFieldValue,yearsFieldValue;

  private void initializeLocksList() {

        try{
           FileInputStream fileIn = new FileInputStream("locksList.ser");
           ObjectInputStream in = new ObjectInputStream(fileIn);
           activeLocksList = new ArrayList<timedLock>((ArrayList<timedLock>)in.readObject());
           in.close();
           fileIn.close();
        }
        catch(ClassNotFoundException e){
            e.printStackTrace();
        }
        catch (FileNotFoundException e) {
            System.out.println("No file found.");
            activeLocksList = new ArrayList<timedLock>();
        }
        catch(IOException e){
            e.printStackTrace();
        }
        activity = !activeLocksList.isEmpty();
    }
  private void saveLocksList(){
      try{
          //Checkout where it ends up
          FileOutputStream fileOut = new FileOutputStream("locksList.ser");
          ObjectOutputStream out = new ObjectOutputStream(fileOut);
          System.out.println(activeLocksList.toString());
          out.writeObject(activeLocksList);
          out.close();
          fileOut.close();
      }
      catch(IOException e)
      {
           e.printStackTrace();
      }

  }
  private boolean lockFile(timedLock tL){
    String workingDir = System.getProperty("user.dir");
    Runtime rt = Runtime.getRuntime();
    if(!Files.exists(Paths.get(workingDir+"\\lf"))){
        try{
            Files.createDirectories(Paths.get(workingDir+"\\lf"));
            Process p = rt.exec("icacls "+workingDir+"\\lf /deny *S-1-1-0:RX");
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }
    try{
        Process openDir = rt.exec("icacls "+workingDir+"\\lf /grant *S-1-1-0:RX");
        openDir.waitFor();
        System.out.println(tL.getFile().getName());
        Files.move(Paths.get(tL.getFile().getPath()), Paths.get(workingDir+"\\lf\\"+tL.getFile().getName()));
        Process lockFile = rt.exec("icacls "+workingDir +"\\lf\\"+(tL.getFile().getName())+" /deny *S-1-1-0:RX");
        lockFile.waitFor();
        Process closeDir = rt.exec("icacls "+workingDir+"\\lf /deny *S-1-1-0:RX");
        closeDir.waitFor();
        return true;
    }
    catch(Exception e){
        e.printStackTrace();
        return false;
    }
    
  }
  private boolean unlockFile(timedLock tL){
      String workingDir = System.getProperty("user.dir");
      Runtime rt = Runtime.getRuntime();
      System.out.println(workingDir);
      
      try{
          Process openDir = rt.exec("icacls "+workingDir+"\\lf /grant *S-1-1-0:RX");
          openDir.waitFor();
          if(!Files.exists(Paths.get(workingDir+"\\lf"))){
            System.out.println("HHAA");
            return false;
          }
          Process lockFile = rt.exec("icacls "+workingDir +"\\lf\\"+(tL.getFile().getName())+" /grant *S-1-1-0:RX");
          lockFile.waitFor();
          Files.move(Paths.get(workingDir+"\\lf\\"+tL.getFile().getName()),Paths.get(tL.getPath()+"\\"+tL.getFile().getName()));
          Process closeDir = rt.exec("icacls "+workingDir+"\\lf /deny *S-1-1-0:RX");
          closeDir.waitFor();
          return true;
      }
      catch(Exception e){
          e.printStackTrace();
          return false;
      }
  }
  private void checkLockEnd(){
      ArrayList<timedLock> toRemove = new ArrayList<timedLock>();
      if(activeLocksList.isEmpty()){
          activity=false;
          return;
      }
      for(timedLock aLock : activeLocksList){
          if(LocalDateTime.now().isAfter(aLock.getEnd())){
              toRemove.add(aLock);
              System.out.println(unlockFile(aLock));
          }
      }
      activeLocksList.removeAll(toRemove);
      if(activeLocksList.isEmpty()){
          activity=false;
          saveLocksList();
          return;
      }
  }
  private LocalDateTime calculateLockDuration(){
    int preSetYear, preSetMonth, preSetDay,preSetHours,preSetMinutes;
    if(getDatePickerValue() != null){
        System.out.println("using date picker");
        preSetYear = getDatePickerValue().getYear();
        preSetMonth = getDatePickerValue().getMonthValue();
        preSetDay = getDatePickerValue().getDayOfMonth();
    }
    else{
        preSetYear = LocalDate.now().getYear();
        preSetMonth = LocalDate.now().getMonthValue();
        preSetDay = LocalDate.now().getDayOfMonth();
    }
    preSetHours=LocalDateTime.now().getHour();
    preSetMinutes=LocalDateTime.now().getMinute();
    LocalDateTime tmp = LocalDateTime.of(preSetYear,preSetMonth,preSetDay,preSetHours,preSetMinutes,0);
    lockDuration = tmp.plusYears(getYearsFieldValue()).plusMonths(getMonthsFieldValue()).plusWeeks(getWeeksFieldValue()).plusDays(getDaysFieldValue()).plusHours(getHoursFieldValue()).plusMinutes(getMinutesFieldValue());

    return lockDuration;
  }

  private  LocalDateTime getLockDuration(){
      if(lockDuration == null){
          return(calculateLockDuration());
      }
      return lockDuration;
  }
  private LocalDate getDatePickerValue(){
    return datePickerValue;
  }
  private Integer getMinutesFieldValue(){
    return minutesFieldValue;
  }
  private Integer getHoursFieldValue(){
    return hoursFieldValue;
  }
  private Integer getDaysFieldValue(){
    return daysFieldValue;
  }
  private Integer getWeeksFieldValue(){
    return weeksFieldValue;
  }
  private Integer getMonthsFieldValue(){
    return monthsFieldValue;
  }
  private Integer getYearsFieldValue(){
    return yearsFieldValue;
  }

  private String displayActiveLocks(){
      
      if(activity==true){
        String activeLocks = "\n";
        for(timedLock aLock : activeLocksList){
            activeLocks = activeLocks+aLock.toString()+" \n\n";
        }
        return activeLocks;
      }
      return "\nNo active lock.";
  }

  private String lockDurationToString(LocalDateTime lock){
   return(lock.getDayOfMonth()+"/"+lock.getMonth()+"/"+lock.getYear()+" at "+lock.getHour()+":"+lock.getMinute());
  }
  private String fileNameToString(File file){
      return file.getName();
  }


  private boolean initAndVerif() {
        boolean allGood = true;
        boolean validTime = false;
        for(int i =0;i<lockComponents.length;i++){
            switch(i){
                case 0:
                    try{
                        if(((DatePicker)lockComponents[i]).getDate().isBefore(java.time.LocalDate.now()) ){
                            
                            allGood =false;
                            lockComponents[i].setBackground(Color.red);
                        }
                        else{
                            lockComponents[i].setBackground(new Color(240,240,240));
                            datePickerValue = ((DatePicker)lockComponents[i]).getDate();
                            validTime = true;
                        }
                    }
                    catch(NullPointerException e){
                        System.out.println("No date picked");
                    }
                    break;
                case 1:
                    if(!((JTextField)lockComponents[i]).getText().isEmpty()){
                        minutesFieldValue = Integer.parseInt(((JTextField)lockComponents[i]).getText());
                        validTime = true;
                    }
                    else{
                        minutesFieldValue = 0;
                    }
                    break;
                case 2:
                    if(!((JTextField)lockComponents[i]).getText().isEmpty()){
                        hoursFieldValue = Integer.parseInt(((JTextField)lockComponents[i]).getText());
                        validTime = true;
                    }
                    else{
                        hoursFieldValue = 0;
                    }
                    break;
                case 3:
                    if(!((JTextField)lockComponents[i]).getText().isEmpty()){
                        daysFieldValue = Integer.parseInt(((JTextField)lockComponents[i]).getText());
                        validTime = true;
                    }
                    else{
                        daysFieldValue = 0;
                    }
                    break;
                case 4:
                    if(!((JTextField)lockComponents[i]).getText().isEmpty()){
                        weeksFieldValue = Integer.parseInt(((JTextField)lockComponents[i]).getText());
                        validTime = true;
                    }
                    else{
                        weeksFieldValue = 0;
                    }
                    break;
                case 5:
                    if(!((JTextField)lockComponents[i]).getText().isEmpty()){
                        monthsFieldValue = Integer.parseInt(((JTextField)lockComponents[i]).getText());
                        validTime = true;
                    }
                    else{
                        monthsFieldValue = 0;
                    }
                    break;
                case 6:
                    if(!((JTextField)lockComponents[i]).getText().isEmpty()){
                        yearsFieldValue = Integer.parseInt(((JTextField)lockComponents[i]).getText());
                        validTime = true;
                    }
                    else{
                        yearsFieldValue = 0;
                    }
                    break;
                case 7:
                    if(((JTextField)lockComponents[i]).getText().equals("Filepath")|| ((JTextField)lockComponents[i]).getText().isEmpty() || (!file.exists() && !file.isDirectory())){
                        allGood =false;
                        lockComponents[i].setBackground(Color.red);
                        lockComponents[i].setForeground(Color.WHITE);
                    }
                    break;
            }
        }
        return(allGood && validTime);
    }

    /**
     * Creates new form TimelockGUI
     */
    public TimelockGUI() {
        initializeLocksList();
        initComponents();
        System.out.println(System.getProperties());
        System.out.println(System.getProperty("user.dir"));
        List<String> wU = new ArrayList<String>();
        for(File uD : new File("C:/Users").listFiles()){
            wU.add(uD.getName());
        }
        System.out.println(wU.toString());
        
        cardManager = (CardLayout)basePanel.getLayout();
        
        System.out.println(activeLocksList.toString());
        lockComponents = new JComponent[]{datePicker,minutesTextField,hoursTextField,daysTextField,weeksTextField,monthsTextField,yearsTextField,filePathDisplay};
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        fileChooser = new javax.swing.JFileChooser();
        confirmDialog = new javax.swing.JDialog();
        cancelButton = new javax.swing.JButton();
        confirmButton = new javax.swing.JButton();
        confirmLabel = new javax.swing.JLabel();
        basePanel = new javax.swing.JPanel();
        homePanel = new javax.swing.JPanel();
        controlPanelHome = new javax.swing.JPanel();
        exitButtonHome = new javax.swing.JButton();
        proceedButton = new javax.swing.JButton();
        refreshButtonHome = new javax.swing.JButton();
        displayPanelContainer = new javax.swing.JPanel();
        activelockScrollPane = new javax.swing.JScrollPane();
        activeLockTextArea = new javax.swing.JTextArea();
        activelockLabel = new javax.swing.JLabel();
        logoPanelContainer = new javax.swing.JPanel();
        tlockLogoLabel = new javax.swing.JLabel();
        tlockLabel = new javax.swing.JLabel();
        mainPanel = new javax.swing.JPanel();
        minutesTextField = new javax.swing.JTextField();
        hoursTextField = new javax.swing.JTextField();
        daysTextField = new javax.swing.JTextField();
        weeksTextField = new javax.swing.JTextField();
        monthsTextField = new javax.swing.JTextField();
        yearsTextField = new javax.swing.JTextField();
        minutesLabel = new javax.swing.JLabel();
        hoursLabel = new javax.swing.JLabel();
        daysLabel = new javax.swing.JLabel();
        weeksLabel = new javax.swing.JLabel();
        monthsLabel = new javax.swing.JLabel();
        yearsLabel = new javax.swing.JLabel();
        fileButton = new javax.swing.JButton();
        filePathDisplay = new javax.swing.JTextField();
        datePicker = new com.github.lgooddatepicker.components.DatePicker();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        controlPanelMain = new javax.swing.JPanel();
        lockButtonMain = new javax.swing.JButton();
        cancelButtonMain = new javax.swing.JButton();
        resetButtonMain = new javax.swing.JButton();

        fileChooser.setDialogTitle("");

        cancelButton.setText("Cancel");
        cancelButton.setBackground(new java.awt.Color(204, 204, 204));
        cancelButton.setFocusPainted(false);
        cancelButton.setFont(new java.awt.Font("Arial", 0, 13)); // NOI18N
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        confirmButton.setText("Confirm");
        confirmButton.setBackground(new java.awt.Color(204, 204, 204));
        confirmButton.setFocusPainted(false);
        confirmButton.setFont(new java.awt.Font("Arial", 0, 13)); // NOI18N
        confirmButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                confirmButtonActionPerformed(evt);
            }
        });

        confirmLabel.setText("jLabel5");
        confirmLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        javax.swing.GroupLayout confirmDialogLayout = new javax.swing.GroupLayout(confirmDialog.getContentPane());
        confirmDialog.getContentPane().setLayout(confirmDialogLayout);
        confirmDialogLayout.setHorizontalGroup(
            confirmDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(confirmDialogLayout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 180, Short.MAX_VALUE)
                .addComponent(confirmButton, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10))
            .addGroup(confirmDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(confirmLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        confirmDialogLayout.setVerticalGroup(
            confirmDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, confirmDialogLayout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addComponent(confirmLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 45, Short.MAX_VALUE)
                .addGroup(confirmDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(confirmButton, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(15, 15, 15))
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Timelock");
        setBackground(new java.awt.Color(224, 224, 224));
        setResizable(false);
        setSize(new java.awt.Dimension(610, 630));

        basePanel.setLayout(new java.awt.CardLayout());

        homePanel.setPreferredSize(new java.awt.Dimension(610, 630));

        controlPanelHome.setBackground(new java.awt.Color(153, 153, 153));
        controlPanelHome.setPreferredSize(new java.awt.Dimension(610, 63));

        exitButtonHome.setText("Exit");
        exitButtonHome.setBackground(new java.awt.Color(204, 204, 204));
        exitButtonHome.setFocusPainted(false);
        exitButtonHome.setFont(new java.awt.Font("Arial", 0, 13)); // NOI18N
        exitButtonHome.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitButtonHomeActionPerformed(evt);
            }
        });

        proceedButton.setText("Proceed");
        proceedButton.setBackground(new java.awt.Color(204, 204, 204));
        proceedButton.setFocusPainted(false);
        proceedButton.setFont(new java.awt.Font("Arial", 0, 13)); // NOI18N
        proceedButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proceedButtonActionPerformed(evt);
            }
        });

        refreshButtonHome.setText("Refresh");
        refreshButtonHome.setBackground(new java.awt.Color(204, 204, 204));
        refreshButtonHome.setFocusPainted(false);
        refreshButtonHome.setFont(new java.awt.Font("Arial", 0, 13)); // NOI18N
        refreshButtonHome.setToolTipText("");
        refreshButtonHome.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshButtonHomeActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout controlPanelHomeLayout = new javax.swing.GroupLayout(controlPanelHome);
        controlPanelHome.setLayout(controlPanelHomeLayout);
        controlPanelHomeLayout.setHorizontalGroup(
            controlPanelHomeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(controlPanelHomeLayout.createSequentialGroup()
                .addGap(35, 35, 35)
                .addComponent(exitButtonHome, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(150, 150, 150)
                .addComponent(refreshButtonHome, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(150, 150, 150)
                .addComponent(proceedButton, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(35, 35, 35))
        );
        controlPanelHomeLayout.setVerticalGroup(
            controlPanelHomeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, controlPanelHomeLayout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(controlPanelHomeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(exitButtonHome, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(proceedButton, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(refreshButtonHome, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(19, Short.MAX_VALUE))
        );

        displayPanelContainer.setBackground(new java.awt.Color(204, 204, 204));
        displayPanelContainer.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        activelockScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        activelockScrollPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        activeLockTextArea.setEditable(false);
        activeLockTextArea.setBackground(new java.awt.Color(230, 230, 230));
        activeLockTextArea.setColumns(20);
        activeLockTextArea.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        activeLockTextArea.setRows(5);
        activeLockTextArea.setText(displayActiveLocks());
        activelockScrollPane.setViewportView(activeLockTextArea);

        activelockLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        activelockLabel.setText("Active locks");
        activelockLabel.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        activelockLabel.setForeground(new java.awt.Color(165, 42, 42));
        activelockLabel.setPreferredSize(new java.awt.Dimension(41, 28));

        javax.swing.GroupLayout displayPanelContainerLayout = new javax.swing.GroupLayout(displayPanelContainer);
        displayPanelContainer.setLayout(displayPanelContainerLayout);
        displayPanelContainerLayout.setHorizontalGroup(
            displayPanelContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, displayPanelContainerLayout.createSequentialGroup()
                .addContainerGap(90, Short.MAX_VALUE)
                .addComponent(activelockLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(90, 90, 90))
            .addComponent(activelockScrollPane)
        );
        displayPanelContainerLayout.setVerticalGroup(
            displayPanelContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, displayPanelContainerLayout.createSequentialGroup()
                .addComponent(activelockLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(activelockScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 480, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        tlockLogoLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/swingTimelock/images/tlocklogo.png"))); // NOI18N

        tlockLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        tlockLabel.setText("<html>Timelock</html>");
        tlockLabel.setFont(new java.awt.Font("Arial", 1, 48)); // NOI18N
        tlockLabel.setForeground(new java.awt.Color(42, 165, 165));

        javax.swing.GroupLayout logoPanelContainerLayout = new javax.swing.GroupLayout(logoPanelContainer);
        logoPanelContainer.setLayout(logoPanelContainerLayout);
        logoPanelContainerLayout.setHorizontalGroup(
            logoPanelContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(logoPanelContainerLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(tlockLogoLabel))
            .addComponent(tlockLabel, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        logoPanelContainerLayout.setVerticalGroup(
            logoPanelContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, logoPanelContainerLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(tlockLogoLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tlockLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout homePanelLayout = new javax.swing.GroupLayout(homePanel);
        homePanel.setLayout(homePanelLayout);
        homePanelLayout.setHorizontalGroup(
            homePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(controlPanelHome, javax.swing.GroupLayout.DEFAULT_SIZE, 620, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, homePanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(logoPanelContainer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(displayPanelContainer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        homePanelLayout.setVerticalGroup(
            homePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, homePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(homePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(displayPanelContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(logoPanelContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 28, Short.MAX_VALUE)
                .addComponent(controlPanelHome, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        basePanel.add(homePanel, "card1");

        mainPanel.setPreferredSize(new java.awt.Dimension(610, 630));

        minutesTextField.setFont(new java.awt.Font("Arial", 0, 13)); // NOI18N
        minutesTextField.setInputVerifier(new inputVerifier());
        minutesTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                minutesTextFieldActionPerformed(evt);
            }
        });

        hoursTextField.setFont(new java.awt.Font("Arial", 0, 13)); // NOI18N
        hoursTextField.setInputVerifier(new inputVerifier());

        daysTextField.setFont(new java.awt.Font("Arial", 0, 13)); // NOI18N
        daysTextField.setInputVerifier(new inputVerifier());
        daysTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                daysTextFieldActionPerformed(evt);
            }
        });

        weeksTextField.setFont(new java.awt.Font("Arial", 0, 13)); // NOI18N
        weeksTextField.setInputVerifier(new inputVerifier());
        weeksTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                weeksTextFieldActionPerformed(evt);
            }
        });

        monthsTextField.setFont(new java.awt.Font("Arial", 0, 13)); // NOI18N
        monthsTextField.setInputVerifier(new inputVerifier());

        yearsTextField.setFont(new java.awt.Font("Arial", 0, 13)); // NOI18N
        yearsTextField.setInputVerifier(new inputVerifier());
        yearsTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                yearsTextFieldActionPerformed(evt);
            }
        });

        minutesLabel.setText(" Minutes");
        minutesLabel.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N

        hoursLabel.setText(" Hours");
        hoursLabel.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N

        daysLabel.setText(" Days");
        daysLabel.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N

        weeksLabel.setText(" Weeks");
        weeksLabel.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N

        monthsLabel.setText(" Months");
        monthsLabel.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N

        yearsLabel.setText(" Years");
        yearsLabel.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N

        fileButton.setText("File");
        fileButton.setBackground(new java.awt.Color(204, 204, 204));
        fileButton.setBorderPainted(false);
        fileButton.setFocusPainted(false);
        fileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileButtonActionPerformed(evt);
            }
        });

        filePathDisplay.setEditable(false);
        filePathDisplay.setHorizontalAlignment(javax.swing.JTextField.LEFT);
        filePathDisplay.setText("Filepath");
        filePathDisplay.setBackground(new java.awt.Color(255, 255, 255));
        filePathDisplay.setFocusable(false);
        filePathDisplay.setFont(new java.awt.Font("Arial", 2, 12)); // NOI18N
        filePathDisplay.setForeground(new java.awt.Color(102, 102, 102));
        filePathDisplay.setInputVerifier(new inputVerifier());
        filePathDisplay.setMinimumSize(new java.awt.Dimension(6, 25));
        filePathDisplay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filePathDisplayActionPerformed(evt);
            }
        });

        datePicker.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        datePicker.setMinimumSize(new java.awt.Dimension(155, 25));
        datePicker.setPreferredSize(new java.awt.Dimension(155, 25));

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel1.setText("<html>Welcome to<br/>TimeLock</html>");
        jLabel1.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel1.setFont(new java.awt.Font("Arial", 1, 30)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(165, 42, 42));
        jLabel1.setMinimumSize(new java.awt.Dimension(140, 70));
        jLabel1.setPreferredSize(new java.awt.Dimension(200, 105));
        jLabel1.setToolTipText("");

        jLabel2.setText("<html>Choose a file and either<br/>the date or duration you<br/>want it to be locked away,<br/>invisible and inaccessible.</html>");
        jLabel2.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel2.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel3.setText("<html>Warning:</html>");
        jLabel3.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        jLabel3.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 0, 0));

        jLabel4.setText("<html>You will not be able to use <br/>the locked file at all.<br/> <br/>Don't use TimeLock on essential files or those necessary on other softwares!</html>");
        jLabel4.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel4.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N

        controlPanelMain.setBackground(new java.awt.Color(153, 153, 153));
        controlPanelMain.setPreferredSize(new java.awt.Dimension(610, 63));

        lockButtonMain.setText("Lock !");
        lockButtonMain.setBackground(new java.awt.Color(204, 204, 204));
        lockButtonMain.setFocusPainted(false);
        lockButtonMain.setFont(new java.awt.Font("Arial", 0, 13)); // NOI18N
        lockButtonMain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lockButtonMainActionPerformed(evt);
            }
        });

        cancelButtonMain.setText("Cancel");
        cancelButtonMain.setBackground(new java.awt.Color(204, 204, 204));
        cancelButtonMain.setFocusPainted(false);
        cancelButtonMain.setFont(new java.awt.Font("Arial", 0, 13)); // NOI18N
        cancelButtonMain.setPreferredSize(new java.awt.Dimension(66, 23));
        cancelButtonMain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonMainActionPerformed(evt);
            }
        });

        resetButtonMain.setText("Reset");
        resetButtonMain.setBackground(new java.awt.Color(204, 204, 204));
        resetButtonMain.setFocusPainted(false);
        resetButtonMain.setFont(new java.awt.Font("Arial", 0, 13)); // NOI18N
        resetButtonMain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetButtonMainActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout controlPanelMainLayout = new javax.swing.GroupLayout(controlPanelMain);
        controlPanelMain.setLayout(controlPanelMainLayout);
        controlPanelMainLayout.setHorizontalGroup(
            controlPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(controlPanelMainLayout.createSequentialGroup()
                .addGap(35, 35, 35)
                .addComponent(cancelButtonMain, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(150, 150, 150)
                .addComponent(lockButtonMain, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(150, 150, 150)
                .addComponent(resetButtonMain, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(35, 35, 35))
        );
        controlPanelMainLayout.setVerticalGroup(
            controlPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, controlPanelMainLayout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(controlPanelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lockButtonMain, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cancelButtonMain, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(resetButtonMain, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10))
        );

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(controlPanelMain, javax.swing.GroupLayout.DEFAULT_SIZE, 620, Short.MAX_VALUE)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 215, Short.MAX_VALUE))
                .addGap(39, 39, 39)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(mainPanelLayout.createSequentialGroup()
                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(weeksLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(monthsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(yearsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(184, 184, 184)
                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(yearsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(monthsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(weeksTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(mainPanelLayout.createSequentialGroup()
                                .addComponent(daysLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(184, 184, 184)
                                .addComponent(daysTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(datePicker, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(minutesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(hoursLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(filePathDisplay, javax.swing.GroupLayout.PREFERRED_SIZE, 229, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(15, 15, 15)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(fileButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(hoursTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 75, Short.MAX_VALUE)
                            .addComponent(minutesTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 75, Short.MAX_VALUE))
                        .addGap(0, 0, Short.MAX_VALUE))))
        );

        mainPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {daysLabel, monthsLabel, weeksLabel, yearsLabel});

        mainPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {daysTextField, weeksTextField, yearsTextField});

        mainPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {hoursTextField, minutesTextField, monthsTextField});

        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                        .addComponent(datePicker, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(20, 20, 20)))
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(yearsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(yearsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(25, 25, 25)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(monthsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(monthsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(25, 25, 25)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(weeksTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(weeksLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(25, 25, 25)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(daysTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(daysLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(25, 25, 25)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(hoursTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(hoursLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(25, 25, 25)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(minutesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(minutesTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGap(42, 42, 42)
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(55, 55, 55)
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(45, 45, 45)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(filePathDisplay, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fileButton, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(controlPanelMain, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        mainPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {daysLabel, monthsLabel, weeksLabel, yearsLabel});

        mainPanelLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {daysTextField, hoursTextField, minutesTextField, monthsTextField, weeksTextField, yearsTextField});

        basePanel.add(mainPanel, "card2");

        getContentPane().add(basePanel, java.awt.BorderLayout.CENTER);

        setSize(new java.awt.Dimension(628, 677));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void filePathDisplayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filePathDisplayActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_filePathDisplayActionPerformed

    private void fileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileButtonActionPerformed
        int returnVal = fileChooser.showOpenDialog(this);
        if(returnVal == JFileChooser.APPROVE_OPTION){
          file = fileChooser.getSelectedFile();
          filePath = file.getAbsolutePath();
          filePathDisplay.setText(filePath);
          if(filePathDisplay.getBackground()==Color.red){
            filePathDisplay.setBackground(Color.white);
            filePathDisplay.setForeground(Color.black);
          }
        }
    }//GEN-LAST:event_fileButtonActionPerformed

    private void cancelButtonMainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonMainActionPerformed
        cardManager.previous(basePanel);
    }//GEN-LAST:event_cancelButtonMainActionPerformed

    private void resetButtonMainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetButtonMainActionPerformed
        for(int i =0;i<lockComponents.length;i++){
            switch(i){
                case 0:
                    ((DatePicker)lockComponents[i]).setDate(null);
                    lockComponents[i].setBackground(new Color(240,240,240));
                    break;
                case 7:
                    ((JTextField)lockComponents[i]).setText("Filepath");
                    lockComponents[i].setBackground(Color.white);
                    lockComponents[i].setForeground(new Color(102,102,102));
                    break;
                default:
                    ((JTextField)lockComponents[i]).setText(null);
                    lockComponents[i].setBackground(Color.white);
                    lockComponents[i].setForeground(Color.black);
                    break;
            }
        }
        lockDuration = null;
    }//GEN-LAST:event_resetButtonMainActionPerformed

    private void lockButtonMainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lockButtonMainActionPerformed
        // TODO add your handling code here:
        System.out.println(evt);
        boolean tmp =initAndVerif();
        if(!tmp)
            Toolkit.getDefaultToolkit().beep();
        else{
            /*System.out.println("Ready to lock");
            System.out.println(calculateLockDuration().toString());*/

            Object[] options = {"Cancel","Lock"};
            int n = JOptionPane.showOptionDialog(mainPanel,
                    "Your lock on: "+fileNameToString(file)+" will end the: "+lockDurationToString(getLockDuration()),
                    "Confirm lock",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    options[0]);
            if(n == 1){
                
                timedLock tl = new timedLock("tL"+fileNameToString(file)+"_"+getLockDuration(),file.getParent(),file,getLockDuration());
                System.out.println(tl.toString());
                boolean result = lockFile(tl);
                if(result){
                    activeLocksList.add(tl);
                    activeLocksList.toString();
                    activity=true;
                    saveLocksList();
                    resetButtonMainActionPerformed(null);
                }
                else{
                    JOptionPane.showMessageDialog(mainPanel, "An error occured while trying to start the locking process", "Error", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
                cardManager.first(basePanel);
            }
        }

    }//GEN-LAST:event_lockButtonMainActionPerformed

    private void weeksTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_weeksTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_weeksTextFieldActionPerformed

    private void daysTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_daysTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_daysTextFieldActionPerformed

    private void yearsTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_yearsTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_yearsTextFieldActionPerformed

    private void exitButtonHomeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitButtonHomeActionPerformed
        // TODO add your handling code here:
        System.exit(0);
    }//GEN-LAST:event_exitButtonHomeActionPerformed

    private void proceedButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proceedButtonActionPerformed
        // TODO add your handling code here:
        cardManager.next(basePanel);
    }//GEN-LAST:event_proceedButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void confirmButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_confirmButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_confirmButtonActionPerformed

    private void refreshButtonHomeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshButtonHomeActionPerformed
        // TODO add your handling code here:
        checkLockEnd();
        activeLockTextArea.setText(displayActiveLocks());
    }//GEN-LAST:event_refreshButtonHomeActionPerformed

    private void minutesTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_minutesTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_minutesTextFieldActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        try{
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch(Exception e){

        }
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new TimelockGUI().setVisible(true);
                   
            }
        });

    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextArea activeLockTextArea;
    private javax.swing.JLabel activelockLabel;
    private javax.swing.JScrollPane activelockScrollPane;
    private javax.swing.JPanel basePanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton cancelButtonMain;
    private javax.swing.JButton confirmButton;
    private javax.swing.JDialog confirmDialog;
    private javax.swing.JLabel confirmLabel;
    private javax.swing.JPanel controlPanelHome;
    private javax.swing.JPanel controlPanelMain;
    private com.github.lgooddatepicker.components.DatePicker datePicker;
    private javax.swing.JLabel daysLabel;
    private javax.swing.JTextField daysTextField;
    private javax.swing.JPanel displayPanelContainer;
    private javax.swing.JButton exitButtonHome;
    private javax.swing.JButton fileButton;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JTextField filePathDisplay;
    private javax.swing.JPanel homePanel;
    private javax.swing.JLabel hoursLabel;
    private javax.swing.JTextField hoursTextField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JButton lockButtonMain;
    private javax.swing.JPanel logoPanelContainer;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JLabel minutesLabel;
    private javax.swing.JTextField minutesTextField;
    private javax.swing.JLabel monthsLabel;
    private javax.swing.JTextField monthsTextField;
    private javax.swing.JButton proceedButton;
    private javax.swing.JButton refreshButtonHome;
    private javax.swing.JButton resetButtonMain;
    private javax.swing.JLabel tlockLabel;
    private javax.swing.JLabel tlockLogoLabel;
    private javax.swing.JLabel weeksLabel;
    private javax.swing.JTextField weeksTextField;
    private javax.swing.JLabel yearsLabel;
    private javax.swing.JTextField yearsTextField;
    // End of variables declaration//GEN-END:variables
}
