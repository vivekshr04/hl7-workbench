package com.eqe.hl7workbench;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.EmptyBorder;

public final class MainWindow {
    private static final Color BRAND_BLUE=new Color(31,78,121), BRAND_BLUE_DARK=new Color(23,55,88);
    private static final Color ACCENT_BLUE=new Color(221,235,247), HEADER_BACKGROUND=new Color(235,241,247);
    private static final Color PANEL_BACKGROUND=new Color(247,249,252), ALTERNATE_ROW=new Color(248,250,252);
    private static final Color GRID_COLOUR=new Color(211,219,229), MUTED_TEXT=new Color(83,96,113);
    private static final Font UI_FONT=new Font("SansSerif",Font.PLAIN,13);
    private static final Font UI_FONT_BOLD=new Font("SansSerif",Font.BOLD,13);
    private static final Font CODE_FONT=new Font(Font.MONOSPACED,Font.PLAIN,13);

    private JTextArea hl7Input;
    private JTextArea lineNumberArea;
    private DefaultTableModel segmentTableModel;
    private JTable segmentTable;
    private final java.util.List<Segment> parsedSegments = new java.util.ArrayList<>();
    private DefaultTableModel fieldModel;
    private DefaultTableModel componentModel;
    private JTable fieldTable;
    private JSplitPane fieldDetailsSplit;
    private JLabel statusLabel;
    private JTabbedPane workspaceTabs;
    private JTextArea compareSourceArea;
    private JTextArea compareTargetArea;
    private JLabel comparisonStatusLabel;
    private final java.util.List<HoverToken> sourceHoverTokens = new java.util.ArrayList<>();
    private final java.util.List<HoverToken> targetHoverTokens = new java.util.ArrayList<>();
    private final Map<String, Integer> occurrenceMap = new HashMap<>();

    /** Opens the application window on the Swing event-dispatch thread. */
    public static void open() {
        SwingUtilities.invokeLater(() -> new MainWindow().createUI());
    }

    private void createUI() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch(Exception ignored) {}
        UIManager.put("Label.font",UI_FONT); UIManager.put("Button.font",UI_FONT_BOLD);
        UIManager.put("Menu.font",UI_FONT); UIManager.put("MenuItem.font",UI_FONT);
        JFrame frame = new JFrame(AppInfo.APP_NAME);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JMenuBar menuBar = new JMenuBar();
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(event -> JOptionPane.showMessageDialog(
                frame,
                AppInfo.APP_NAME
                        + "\nVersion: " + AppInfo.VERSION
                        + "\nAuthor: " + AppInfo.AUTHOR,
                "About",
                JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);
        frame.setJMenuBar(menuBar);

        // Top panel: full-width raw HL7 editor.
        hl7Input = new JTextArea();
        hl7Input.setFont(CODE_FONT); hl7Input.setLineWrap(false); hl7Input.setTabSize(4);
        hl7Input.setBackground(Color.WHITE); hl7Input.setForeground(new Color(32,42,55));
        hl7Input.setCaretColor(BRAND_BLUE); hl7Input.setSelectionColor(ACCENT_BLUE);
        hl7Input.setSelectedTextColor(Color.BLACK); hl7Input.setMargin(new Insets(8,10,8,10));

        lineNumberArea = new JTextArea("1");
        lineNumberArea.setEditable(false);
        lineNumberArea.setFocusable(false);
        lineNumberArea.setFont(CODE_FONT); lineNumberArea.setBackground(new Color(238,243,248));
        lineNumberArea.setForeground(MUTED_TEXT);
        lineNumberArea.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));

        hl7Input.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent event) {
                refreshLineNumbers();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent event) {
                refreshLineNumbers();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent event) {
                refreshLineNumbers();
            }
        });

        JScrollPane messageScrollPane=new JScrollPane(hl7Input); messageScrollPane.setRowHeaderView(lineNumberArea);
        messageScrollPane.setBorder(BorderFactory.createLineBorder(GRID_COLOUR));

        JPanel inputPanel = titled("Paste or Edit HL7 Message");
        inputPanel.add(messageScrollPane, BorderLayout.CENTER);

        // Bottom-left panel: segment navigation grid.
        segmentTableModel = readOnlyModel("Seq", "Segment", "Description");
        segmentTable = new JTable(segmentTableModel);
        configureSegmentTable(segmentTable);
        segmentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        segmentTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) showFields();
        });
        JPanel segmentPanel = titled("Segments");
        segmentPanel.add(new JScrollPane(segmentTable), BorderLayout.CENTER);

        // Bottom-right upper panel: fields.
        fieldModel = readOnlyModel("Field", "Description", "Value");
        fieldTable = new JTable(fieldModel);
        configureTable(fieldTable, 150, 290, 630);
        installCopySupport(fieldTable);
        fieldTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fieldTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                showComponents();
            }
        });

        JPanel fieldsPanel = titled("Fields");
        fieldsPanel.add(new JScrollPane(fieldTable), BorderLayout.CENTER);

        // Bottom-right lower panel: selected field breakdown.
        componentModel = readOnlyModel(
                "Component / Subcomponent",
                "Description",
                "Value");
        JTable componentTable = new JTable(componentModel);
        configureTable(componentTable, 210, 300, 520);
        installCopySupport(componentTable);

        JPanel componentsPanel = titled("Selected Field Breakdown");
        componentsPanel.add(new JScrollPane(componentTable), BorderLayout.CENTER);
        componentsPanel.setMinimumSize(new Dimension(200, 180));

        fieldDetailsSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                fieldsPanel,
                componentsPanel);
        fieldDetailsSplit.setResizeWeight(0.68);
        fieldDetailsSplit.setDividerLocation(300);
        fieldDetailsSplit.setOneTouchExpandable(true);
        fieldDetailsSplit.setContinuousLayout(true);

        JSplitPane lowerSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                segmentPanel,
                fieldDetailsSplit);
        lowerSplit.setResizeWeight(0.25);
        lowerSplit.setDividerLocation(360);
        lowerSplit.setOneTouchExpandable(true);
        lowerSplit.setContinuousLayout(true);

        // Main layout: message across the top, navigation/details across the bottom.
        JSplitPane mainSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                inputPanel,
                lowerSplit);
        mainSplit.setResizeWeight(0.52);
        mainSplit.setDividerLocation(420);
        mainSplit.setOneTouchExpandable(true);
        mainSplit.setContinuousLayout(true);

        statusLabel=new JLabel("Ready"); statusLabel.setForeground(MUTED_TEXT);
        statusLabel.setFont(new Font("SansSerif",Font.PLAIN,12)); statusLabel.setBorder(new EmptyBorder(4,10,4,10));
        JPanel statusPanel=new JPanel(new BorderLayout()); statusPanel.setBackground(new Color(243,246,249));
        statusPanel.setBorder(BorderFactory.createMatteBorder(1,0,0,0,GRID_COLOUR));
        statusPanel.add(statusLabel, BorderLayout.WEST);
        JPanel viewerWorkspace = new JPanel(new BorderLayout());
        viewerWorkspace.add(createActionToolbar(), BorderLayout.NORTH);
        viewerWorkspace.add(mainSplit, BorderLayout.CENTER);
        workspaceTabs = new JTabbedPane();
        workspaceTabs.setFont(UI_FONT_BOLD);
        workspaceTabs.addTab("Message Viewer", viewerWorkspace);
        workspaceTabs.addTab("Message Comparison", createComparisonWorkspace());
        frame.add(createBrandHeader(), BorderLayout.NORTH);
        frame.add(workspaceTabs, BorderLayout.CENTER);
        frame.add(statusPanel, BorderLayout.SOUTH);
        frame.setMinimumSize(new Dimension(1100, 720));
        frame.setSize(1500, 900);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel titled(String title) {
        JPanel panel=new JPanel(new BorderLayout(6,6)); panel.setBackground(PANEL_BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(GRID_COLOUR),title,
                javax.swing.border.TitledBorder.LEADING,javax.swing.border.TitledBorder.TOP,
                UI_FONT_BOLD,BRAND_BLUE_DARK),new EmptyBorder(4,4,4,4)));
        return panel;
    }
    private JPanel createBrandHeader() {
        JPanel header=new JPanel(new BorderLayout(12,0)); header.setBackground(BRAND_BLUE);
        header.setBorder(new EmptyBorder(12,18,12,18));
        JLabel mark=new JLabel("HL7",SwingConstants.CENTER); mark.setOpaque(true); mark.setBackground(Color.WHITE);
        mark.setForeground(BRAND_BLUE); mark.setFont(new Font("SansSerif",Font.BOLD,18));
        mark.setPreferredSize(new Dimension(52,38)); mark.setBorder(BorderFactory.createLineBorder(new Color(205,220,235)));
        JPanel titles=new JPanel(); titles.setOpaque(false); titles.setLayout(new BoxLayout(titles,BoxLayout.Y_AXIS));
        JLabel title=new JLabel(AppInfo.APP_NAME); title.setForeground(Color.WHITE); title.setFont(new Font("SansSerif",Font.BOLD,20));
        JLabel subtitle=new JLabel("HL7 Message Viewer & Workbench"); subtitle.setForeground(new Color(218,231,243));
        subtitle.setFont(new Font("SansSerif",Font.PLAIN,12)); titles.add(title); titles.add(Box.createVerticalStrut(2)); titles.add(subtitle);
        JLabel version=new JLabel("Version "+AppInfo.VERSION); version.setForeground(new Color(218,231,243)); version.setFont(UI_FONT);
        header.add(mark,BorderLayout.WEST); header.add(titles,BorderLayout.CENTER); header.add(version,BorderLayout.EAST); return header;
    }
    private JToolBar createActionToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setRollover(true);
        toolbar.setBackground(new Color(243, 246, 249));
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, GRID_COLOUR));
        toolbar.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton parseButton = createPrimaryButton("Parse");
        parseButton.setToolTipText("Parse or refresh the HL7 message structure");
        parseButton.addActionListener(event -> parseMessage());
        JButton copyButton = createToolbarButton("Copy");
        copyButton.addActionListener(event -> copyToClipboard(hl7Input.getText()));
        JButton clearButton = createToolbarButton("Clear");
        clearButton.addActionListener(event -> clearWorkbench());
        toolbar.add(parseButton);
        toolbar.add(copyButton);
        toolbar.add(clearButton);
        return toolbar;
    }



    private JPanel createComparisonWorkspace() {
        compareSourceArea = createComparisonTextArea(true);
        compareTargetArea = createComparisonTextArea(false);
        comparisonStatusLabel = new JLabel("Paste Source and Target messages, then select Compare.");
        comparisonStatusLabel.setForeground(MUTED_TEXT);
        comparisonStatusLabel.setBorder(new EmptyBorder(4, 10, 4, 10));
        JPanel sourcePanel = titled("Source / Input Message");
        sourcePanel.add(new JScrollPane(compareSourceArea), BorderLayout.CENTER);
        JPanel targetPanel = titled("Target / Output Message");
        targetPanel.add(new JScrollPane(compareTargetArea), BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, sourcePanel, targetPanel);
        split.setResizeWeight(0.5); split.setDividerLocation(330);
        split.setContinuousLayout(true); split.setOneTouchExpandable(true);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(createComparisonToolbar(), BorderLayout.NORTH);
        panel.add(split, BorderLayout.CENTER);
        panel.add(comparisonStatusLabel, BorderLayout.SOUTH);
        return panel;
    }

    private JToolBar createComparisonToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false); toolbar.setRollover(true);
        toolbar.setBackground(new Color(243, 246, 249));
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, GRID_COLOUR));
        toolbar.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton compare = createPrimaryButton("Compare");
        compare.addActionListener(event -> compareMessages());
        JButton copySource = createToolbarButton("Copy Source");
        copySource.addActionListener(event -> copyToClipboard(compareSourceArea.getText()));
        JButton copyTarget = createToolbarButton("Copy Target");
        copyTarget.addActionListener(event -> copyToClipboard(compareTargetArea.getText()));
        JButton swap = createToolbarButton("Swap");
        swap.addActionListener(event -> {String value=compareSourceArea.getText();compareSourceArea.setText(compareTargetArea.getText());compareTargetArea.setText(value);clearComparisonHighlights();});
        JButton clear = createToolbarButton("Clear");
        clear.addActionListener(event -> {compareSourceArea.setText("");compareTargetArea.setText("");clearComparisonHighlights();});
        toolbar.add(compare);toolbar.add(copySource);toolbar.add(copyTarget);toolbar.addSeparator(new Dimension(14,24));toolbar.add(swap);toolbar.add(clear);
        return toolbar;
    }

    private JTextArea createComparisonTextArea(boolean sourceArea) {
        JTextArea area = new JTextArea() {
            @Override public String getToolTipText(java.awt.event.MouseEvent event) {
                int offset=viewToModel2D(event.getPoint());
                java.util.List<HoverToken> tokens=sourceArea?sourceHoverTokens:targetHoverTokens;
                for(HoverToken token:tokens) if(offset>=token.startOffset&&offset<=token.endOffset)return token.path;
                return null;
            }
        };
        ToolTipManager.sharedInstance().registerComponent(area);
        area.setFont(CODE_FONT);area.setLineWrap(false);area.setMargin(new Insets(8,10,8,10));
        area.setBackground(Color.WHITE);area.setForeground(new Color(32,42,55));area.setCaretColor(BRAND_BLUE);return area;
    }

    private void compareMessages() {
        clearComparisonHighlights();
        if(compareSourceArea.getText().isBlank()||compareTargetArea.getText().isBlank()){comparisonStatusLabel.setText("Both Source and Target messages are required.");return;}
        compareSourceArea.setText(normaliseComparisonText(compareSourceArea.getText()));
        compareTargetArea.setText(normaliseComparisonText(compareTargetArea.getText()));
        java.util.List<ComparisonSegment> sources=parseComparisonSegments(compareSourceArea.getText());
        java.util.List<ComparisonSegment> targets=parseComparisonSegments(compareTargetArea.getText());
        Map<String,java.util.ArrayDeque<ComparisonSegment>> targetByKey=new HashMap<>();
        for(ComparisonSegment segment:targets)targetByKey.computeIfAbsent(segment.matchKey,k->new java.util.ArrayDeque<>()).add(segment);
        javax.swing.text.Highlighter.HighlightPainter green=new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(new Color(211,242,218));
        javax.swing.text.Highlighter.HighlightPainter red=new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(new Color(255,190,190));
        javax.swing.text.Highlighter.HighlightPainter missing=new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(new Color(255,145,145));
        int matches=0,changes=0,missingSegments=0;
        for(ComparisonSegment source:sources){java.util.ArrayDeque<ComparisonSegment> queue=targetByKey.get(source.matchKey);ComparisonSegment target=queue==null?null:queue.pollFirst();
            if(target==null){highlightRange(compareSourceArea,source.startOffset,source.endOffset,missing);sourceHoverTokens.add(new HoverToken(source.matchKey,source.startOffset,source.endOffset));missingSegments++;continue;}
            java.util.Set<String> paths=new java.util.LinkedHashSet<>();paths.addAll(source.parts.keySet());paths.addAll(target.parts.keySet());
            for(String path:paths){ComparisonToken left=source.parts.get(path),right=target.parts.get(path);String fullPath=source.matchKey+"."+path;
                if(left==null||right==null){if(left!=null){highlightToken(compareSourceArea,left,red);sourceHoverTokens.add(new HoverToken(fullPath,left.startOffset,left.endOffset));}if(right!=null){highlightToken(compareTargetArea,right,red);targetHoverTokens.add(new HoverToken(fullPath,right.startOffset,right.endOffset));}changes++;}
                else if(left.value.equals(right.value)){highlightToken(compareSourceArea,left,green);highlightToken(compareTargetArea,right,green);sourceHoverTokens.add(new HoverToken(fullPath,left.startOffset,left.endOffset));targetHoverTokens.add(new HoverToken(fullPath,right.startOffset,right.endOffset));matches++;}
                else{highlightToken(compareSourceArea,left,red);highlightToken(compareTargetArea,right,red);sourceHoverTokens.add(new HoverToken(fullPath,left.startOffset,left.endOffset));targetHoverTokens.add(new HoverToken(fullPath,right.startOffset,right.endOffset));changes++;}
            }}
        comparisonStatusLabel.setText("Comparison complete | Matching values: "+matches+" | Changed values: "+changes+" | Source segments missing in Target: "+missingSegments+" | Target-only segments ignored");
    }

    private String normaliseComparisonText(String message){return message.replace("\r\n","\n").replace('\r','\n');}
    private java.util.List<ComparisonSegment> parseComparisonSegments(String message){
        java.util.List<ComparisonSegment> result=new java.util.ArrayList<>();Map<String,Integer> occurrences=new HashMap<>();int offset=0;
        for(String line:message.split("\n",-1)){int lineStart=offset,lineEnd=lineStart+line.length();offset=lineEnd+1;int separator=line.indexOf('|');if(separator<=0)continue;String code=line.substring(0,separator).trim();if(code.isEmpty())continue;int occurrence=occurrences.merge(code,1,Integer::sum);String[] values=line.split("\\|",-1);String setId=values.length>1&&values[1].trim().matches("\\d+")?values[1].trim():String.valueOf(occurrence);ComparisonSegment segment=new ComparisonSegment(code+"["+setId+"]",lineStart,lineEnd);
            if(code.equals("MSH")&&line.length()>3)segment.parts.put("1",new ComparisonToken(String.valueOf(line.charAt(3)),lineStart+3,lineStart+4));int tokenStart=separator+1,tokenIndex=1;
            for(int i=separator+1;i<=line.length();i++)if(i==line.length()||line.charAt(i)=='|'){int field=code.equals("MSH")?tokenIndex+1:tokenIndex;String value=line.substring(tokenStart,i);if(code.equals("MSH")&&field==2)segment.parts.put("2",new ComparisonToken(value,lineStart+tokenStart,lineStart+i));else addNestedComparisonParts(segment,String.valueOf(field),value,lineStart+tokenStart);tokenIndex++;tokenStart=i+1;}result.add(segment);}return result;
    }
    private void addNestedComparisonParts(ComparisonSegment segment,String fieldPath,String fieldValue,int absoluteStart){java.util.List<TokenSlice> reps=splitWithOffsets(fieldValue,'~',absoluteStart);boolean repeated=reps.size()>1;for(int r=0;r<reps.size();r++){TokenSlice rep=reps.get(r);String repPath=repeated?fieldPath+"["+(r+1)+"]":fieldPath;java.util.List<TokenSlice> comps=splitWithOffsets(rep.value,'^',rep.startOffset);boolean componentBased=comps.size()>1;for(int c=0;c<comps.size();c++){TokenSlice comp=comps.get(c);String compPath=componentBased?repPath+"."+(c+1):repPath;java.util.List<TokenSlice> subs=splitWithOffsets(comp.value,'&',comp.startOffset);boolean subBased=subs.size()>1;if(subBased)for(int x=0;x<subs.size();x++){TokenSlice sub=subs.get(x);segment.parts.put(compPath+"."+(x+1),new ComparisonToken(sub.value,sub.startOffset,sub.endOffset));}else segment.parts.put(compPath,new ComparisonToken(comp.value,comp.startOffset,comp.endOffset));}}}
    private java.util.List<TokenSlice> splitWithOffsets(String value,char separator,int absoluteStart){java.util.List<TokenSlice> result=new java.util.ArrayList<>();int start=0;for(int i=0;i<=value.length();i++)if(i==value.length()||value.charAt(i)==separator){result.add(new TokenSlice(value.substring(start,i),absoluteStart+start,absoluteStart+i));start=i+1;}return result;}
    private void highlightToken(JTextArea area,ComparisonToken token,javax.swing.text.Highlighter.HighlightPainter painter){int end=token.endOffset<=token.startOffset?Math.min(area.getDocument().getLength(),token.startOffset+1):token.endOffset;highlightRange(area,token.startOffset,end,painter);}
    private void highlightRange(JTextArea area,int start,int end,javax.swing.text.Highlighter.HighlightPainter painter){try{area.getHighlighter().addHighlight(start,end,painter);}catch(javax.swing.text.BadLocationException ignored){}}
    private void clearComparisonHighlights(){if(compareSourceArea!=null)compareSourceArea.getHighlighter().removeAllHighlights();if(compareTargetArea!=null)compareTargetArea.getHighlighter().removeAllHighlights();sourceHoverTokens.clear();targetHoverTokens.clear();}
    private static class ComparisonSegment{final String matchKey;final int startOffset,endOffset;final Map<String,ComparisonToken> parts=new java.util.LinkedHashMap<>();ComparisonSegment(String k,int s,int e){matchKey=k;startOffset=s;endOffset=e;}}
    private static class ComparisonToken{final String value;final int startOffset,endOffset;ComparisonToken(String v,int s,int e){value=v;startOffset=s;endOffset=e;}}
    private static class TokenSlice{final String value;final int startOffset,endOffset;TokenSlice(String v,int s,int e){value=v;startOffset=s;endOffset=e;}}
    private static class HoverToken{final String path;final int startOffset,endOffset;HoverToken(String p,int s,int e){path=p;startOffset=s;endOffset=e;}}
    private JButton createToolbarButton(String text) {
        JButton button = new JButton(text);
        button.setFont(UI_FONT_BOLD);
        button.setForeground(BRAND_BLUE_DARK);
        button.setBackground(Color.WHITE);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GRID_COLOUR),
                new EmptyBorder(6, 14, 6, 14)));
        return button;
    }

    private void clearWorkbench() {
        if (hl7Input.getText().isBlank()) {
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                null,
                "Clear the current HL7 message?",
                AppInfo.APP_NAME,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        hl7Input.setText("");
        parsedSegments.clear();
        segmentTableModel.setRowCount(0);
        fieldModel.setRowCount(0);
        componentModel.setRowCount(0);
        occurrenceMap.clear();
        statusLabel.setText("Ready");
    }

    private JButton createPrimaryButton(String text) {
        JButton b=new JButton(text); b.setFont(UI_FONT_BOLD); b.setForeground(Color.WHITE); b.setBackground(BRAND_BLUE);
        b.setOpaque(true); b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BRAND_BLUE_DARK),
                new EmptyBorder(7,18,7,18))); b.setFocusPainted(false); return b;
    }

    private DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void configureTable(JTable table,int firstWidth,int secondWidth,int thirdWidth) {
        applyProfessionalTableStyle(table); table.getColumnModel().getColumn(0).setPreferredWidth(firstWidth);
        table.getColumnModel().getColumn(1).setPreferredWidth(secondWidth); table.getColumnModel().getColumn(2).setPreferredWidth(thirdWidth);
    }
    private void configureSegmentTable(JTable table) {
        applyProfessionalTableStyle(table); table.getColumnModel().getColumn(0).setPreferredWidth(55);
        table.getColumnModel().getColumn(0).setMaxWidth(70); table.getColumnModel().getColumn(1).setPreferredWidth(105);
        table.getColumnModel().getColumn(2).setPreferredWidth(250);
    }
    private void applyProfessionalTableStyle(JTable table) {
        table.setFont(CODE_FONT); table.setRowHeight(29); table.setShowGrid(true); table.setGridColor(GRID_COLOUR);
        table.setIntercellSpacing(new Dimension(1,1)); table.setFillsViewportHeight(true);
        table.setSelectionBackground(ACCENT_BLUE); table.setSelectionForeground(Color.BLACK);
        table.getTableHeader().setFont(UI_FONT_BOLD); table.getTableHeader().setBackground(HEADER_BACKGROUND);
        table.getTableHeader().setForeground(BRAND_BLUE_DARK); table.getTableHeader().setPreferredSize(new Dimension(0,31));
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable t,Object value,boolean selected,boolean focus,int row,int col){
                Component c=super.getTableCellRendererComponent(t,value,selected,focus,row,col);
                c.setBackground(selected?ACCENT_BLUE:(row%2==0?Color.WHITE:ALTERNATE_ROW)); c.setForeground(new Color(32,42,55));
                if(c instanceof JLabel label) label.setBorder(new EmptyBorder(0,7,0,7)); return c;
            }});
    }

    private void installCopySupport(JTable table) {
        int menuShortcut = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        KeyStroke copyKey = KeyStroke.getKeyStroke('C', menuShortcut);

        table.getInputMap(JComponent.WHEN_FOCUSED).put(copyKey, "copyCellValue");
        table.getActionMap().put("copyCellValue", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                copySelectedCellValue(table);
            }
        });

        JMenuItem copyValueItem = new JMenuItem("Copy Value");
        copyValueItem.addActionListener(event -> copySelectedCellValue(table));

        JMenuItem copyRowItem = new JMenuItem("Copy Full Row");
        copyRowItem.addActionListener(event -> copySelectedRow(table));

        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(copyValueItem);
        popupMenu.add(copyRowItem);
        table.setComponentPopupMenu(popupMenu);

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent event) {
                selectPopupCell(event);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent event) {
                selectPopupCell(event);
            }

            private void selectPopupCell(java.awt.event.MouseEvent event) {
                if (!event.isPopupTrigger()) {
                    return;
                }

                int row = table.rowAtPoint(event.getPoint());
                int column = table.columnAtPoint(event.getPoint());

                if (row >= 0) {
                    table.setRowSelectionInterval(row, row);
                }
                if (column >= 0) {
                    table.setColumnSelectionInterval(column, column);
                }
            }
        });
    }

    private void copySelectedCellValue(JTable table) {
        int row = table.getSelectedRow();
        int column = table.getSelectedColumn();

        if (row < 0) {
            return;
        }

        if (column < 0) {
            column = Math.min(2, table.getColumnCount() - 1);
        }

        Object value = table.getValueAt(row, column);
        copyToClipboard(value == null ? "" : value.toString());
    }

    private void copySelectedRow(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }

        StringBuilder text = new StringBuilder();
        for (int column = 0; column < table.getColumnCount(); column++) {
            if (column > 0) {
                text.append('\t');
            }
            Object value = table.getValueAt(row, column);
            if (value != null) {
                text.append(value);
            }
        }

        copyToClipboard(text.toString());
    }

    private void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(text), null);

        if (statusLabel != null) {
            statusLabel.setText("Copied to clipboard");
        }
    }

    private void refreshLineNumbers() {
        int lineCount = hl7Input.getLineCount();
        StringBuilder numbers = new StringBuilder();

        for (int line = 1; line <= lineCount; line++) {
            numbers.append(line);
            if (line < lineCount) {
                numbers.append(System.lineSeparator());
            }
        }

        lineNumberArea.setText(numbers.toString());
    }

    private void parseMessage() {
        parsedSegments.clear();
        segmentTableModel.setRowCount(0);
        fieldModel.setRowCount(0);
        componentModel.setRowCount(0);
        occurrenceMap.clear();

        String text = hl7Input.getText();
        if (text == null || text.isBlank()) {
            warn("Please paste an HL7 message.");
            return;
        }

        text = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = text.split("\n", -1);

        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String raw = lines[lineIndex];
            if (raw.isBlank()) {
                continue;
            }

            int separatorPosition = raw.indexOf('|');
            if (separatorPosition <= 0) {
                continue;
            }

            String code = raw.substring(0, separatorPosition).trim();
            if (code.isEmpty()) {
                continue;
            }

            int occurrence = occurrenceMap.merge(code, 1, Integer::sum);
            int sequenceNumber = lineIndex + 1;
            String setId = extractSetId(raw, occurrence);

            Segment segment = new Segment(code, sequenceNumber, setId, raw);
            parsedSegments.add(segment);
            segmentTableModel.addRow(new Object[]{sequenceNumber, segment.fieldPath(), Dictionary.segmentName(code)});
        }

        if (parsedSegments.isEmpty()) {
            warn("No valid pipe-delimited HL7 segments were found.");
            statusLabel.setText("No valid HL7 segments found");
        } else {
            segmentTable.setRowSelectionInterval(0, 0);
            statusLabel.setText("Parsed successfully | Segments: " + parsedSegments.size());
        }
    }

    private String extractSetId(String segmentLine, int fallbackOccurrence) {
        String[] fields = segmentLine.split("\\|", -1);

        if (fields.length > 1) {
            String firstField = fields[1].trim();
            if (firstField.matches("\\d+")) {
                return firstField;
            }
        }

        return String.valueOf(fallbackOccurrence);
    }

    private void showFields() {
        fieldModel.setRowCount(0);
        componentModel.setRowCount(0);

        int selectedRow = segmentTable.getSelectedRow();
        if (selectedRow < 0 || selectedRow >= parsedSegments.size()) return;
        Segment segment = parsedSegments.get(selectedRow);

        Delimiters delimiters = delimiters();
        String[] fields = segment.raw.split(
                Pattern.quote(String.valueOf(delimiters.field)),
                -1);

        if (segment.code.equals("MSH")) {
            addField(
                    segment.fieldPath() + ".1",
                    Dictionary.fieldName("MSH.1"),
                    String.valueOf(delimiters.field));
            addField(
                    segment.fieldPath() + ".2",
                    Dictionary.fieldName("MSH.2"),
                    fields.length > 1 ? fields[1] : "");
        }

        for (int index = 1; index < fields.length; index++) {
            int fieldNumber = segment.code.equals("MSH") ? index + 1 : index;

            if (segment.code.equals("MSH") && fieldNumber == 2) {
                continue;
            }

            String standardPath = segment.code + "." + fieldNumber;
            addField(
                    segment.fieldPath() + "." + fieldNumber,
                    Dictionary.fieldName(standardPath),
                    fields[index]);
        }

        statusLabel.setText("Selected: " + segment.path());
    }

    private void addField(String path, String description, String value) {
        fieldModel.addRow(new Object[]{
                path,
                description,
                visible(value)
        });
    }

    private void showComponents() {
        componentModel.setRowCount(0);

        int row = fieldTable.getSelectedRow();
        if (row < 0) {
            return;
        }

        String fieldPath = String.valueOf(fieldModel.getValueAt(row, 0));
        String value = String.valueOf(fieldModel.getValueAt(row, 2));
        if (value.equals("<empty>")) {
            value = "";
        }

        Delimiters delimiters = delimiters();
        String[] repetitions = value.split(
                Pattern.quote(String.valueOf(delimiters.repetition)),
                -1);

        for (int repetitionIndex = 0;
                repetitionIndex < repetitions.length;
                repetitionIndex++) {

            String repetitionPath = repetitions.length > 1
                    ? fieldPath + "[" + (repetitionIndex + 1) + "]"
                    : fieldPath;

            if (repetitions.length > 1) {
                componentModel.addRow(new Object[]{
                        repetitionPath,
                        "Repetition " + (repetitionIndex + 1),
                        visible(repetitions[repetitionIndex])
                });
            }

            String[] components = repetitions[repetitionIndex].split(
                    Pattern.quote(String.valueOf(delimiters.component)),
                    -1);

            for (int componentIndex = 0;
                    componentIndex < components.length;
                    componentIndex++) {

                String componentPath = repetitionPath
                        + "."
                        + (componentIndex + 1);

                componentModel.addRow(new Object[]{
                        componentPath,
                        Dictionary.componentName(
                                basePath(fieldPath),
                                componentIndex + 1),
                        visible(components[componentIndex])
                });

                String[] subcomponents = components[componentIndex].split(
                        Pattern.quote(String.valueOf(delimiters.subcomponent)),
                        -1);

                if (subcomponents.length > 1) {
                    for (int subcomponentIndex = 0;
                            subcomponentIndex < subcomponents.length;
                            subcomponentIndex++) {

                        componentModel.addRow(new Object[]{
                                componentPath + "." + (subcomponentIndex + 1),
                                "Subcomponent " + (subcomponentIndex + 1),
                                visible(subcomponents[subcomponentIndex])
                        });
                    }
                }
            }
        }
    
        revealBreakdownPanel();
    }
    private void revealBreakdownPanel(){SwingUtilities.invokeLater(()->{if(fieldDetailsSplit==null)return;int total=fieldDetailsSplit.getHeight();if(total<=0)return;int lower=total-fieldDetailsSplit.getDividerLocation()-fieldDetailsSplit.getDividerSize();if(lower<180){int desired=Math.min(260,Math.max(200,total/3));fieldDetailsSplit.setDividerLocation(Math.max(140,total-desired-fieldDetailsSplit.getDividerSize()));fieldDetailsSplit.revalidate();fieldDetailsSplit.repaint();}});}


    private String basePath(String displayPath) {
        return displayPath.replaceFirst("\\[[^\\]]+\\]", "");
    }

    private String visible(String value) {
        return value.isEmpty() ? "<empty>" : value;
    }

    private void warn(String text) {
        JOptionPane.showMessageDialog(
                null,
                text,
                AppInfo.APP_NAME,
                JOptionPane.WARNING_MESSAGE);
    }

    private Delimiters delimiters() {
        for (Segment segment : parsedSegments) {
            if (segment.code.equals("MSH") && segment.raw.length() >= 8) {
                return new Delimiters(
                        segment.raw.charAt(3),
                        segment.raw.charAt(4),
                        segment.raw.charAt(5),
                        segment.raw.charAt(7));
            }
        }

        return new Delimiters('|', '^', '~', '&');
    }

    record Delimiters(
            char field,
            char component,
            char repetition,
            char subcomponent) {
    }

    static class Segment {
        final String code;
        final String raw;
        final int sequenceNumber;
        final String setId;

        Segment(String code, int sequenceNumber, String setId, String raw) {
            this.code = code;
            this.sequenceNumber = sequenceNumber;
            this.setId = setId;
            this.raw = raw;
        }

        String path() {
            return sequenceNumber + "." + code + "[" + setId + "]";
        }

        String fieldPath() {
            return code + "[" + setId + "]";
        }

        @Override
        public String toString() {
            return path() + " - " + Dictionary.segmentName(code);
        }
    }

    static class Dictionary {
        private static final Map<String, String> SEGMENTS =
                DictionaryLoader.loadDictionary(
                        "resources/dictionaries/segment_dictionary.csv");

        private static final Map<String, String> FIELDS =
                DictionaryLoader.loadDictionary(
                        "resources/dictionaries/field_dictionary.csv");

        private static final Map<String, String[]> COMPONENTS = new HashMap<>();

        static {
            COMPONENTS.put("MSH.9", new String[]{
                    "Message Code", "Trigger Event", "Message Structure"
            });
            COMPONENTS.put("PID.3", new String[]{
                    "ID Number", "Check Digit", "Check Digit Scheme",
                    "Assigning Authority", "Identifier Type Code",
                    "Assigning Facility", "Effective Date", "Expiration Date",
                    "Assigning Jurisdiction", "Assigning Agency or Department"
            });
            COMPONENTS.put("PID.5", new String[]{
                    "Family Name", "Given Name",
                    "Second and Further Given Names or Initials", "Suffix",
                    "Prefix", "Degree", "Name Type Code",
                    "Name Representation Code", "Name Context",
                    "Name Validity Range", "Name Assembly Order",
                    "Effective Date", "Expiration Date", "Professional Suffix"
            });
            COMPONENTS.put("OBR.4", new String[]{
                    "Identifier", "Text", "Name of Coding System",
                    "Alternate Identifier", "Alternate Text",
                    "Alternate Coding System"
            });
            COMPONENTS.put("OBX.3", new String[]{
                    "Identifier", "Text", "Name of Coding System",
                    "Alternate Identifier", "Alternate Text",
                    "Alternate Coding System"
            });
        }

        static String segmentName(String code) {
            return SEGMENTS.getOrDefault(
                    code,
                    code.startsWith("Z")
                            ? "Custom Z Segment"
                            : "HL7 Segment");
        }

        static String fieldName(String path) {
            return FIELDS.getOrDefault(
                    path,
                    "Field " + path.substring(path.indexOf('.') + 1));
        }

        static String componentName(
        String fieldPath,
        int number) {

    String componentPath =
            fieldPath + "." + number;

    String csvDescription =
            FIELDS.get(componentPath);

    if (csvDescription != null
            && !csvDescription.isBlank()) {

        return csvDescription;
    }

    String[] names =
            COMPONENTS.get(fieldPath);

    if (names != null
            && number <= names.length) {

        return names[number - 1];
    }

    return "Component " + number;
}
        }
    }

