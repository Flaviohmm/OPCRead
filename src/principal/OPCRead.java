/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package principal;

import Jama.Matrix;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafish.clients.opc.JOpc;
import javafish.clients.opc.component.OpcGroup;
import javafish.clients.opc.component.OpcItem;
import javafish.clients.opc.exception.ComponentNotFoundException;
import javafish.clients.opc.exception.ConnectivityException;
import javafish.clients.opc.exception.SynchReadException;
import javafish.clients.opc.exception.SynchWriteException;
import javafish.clients.opc.exception.UnableAddGroupException;
import javafish.clients.opc.exception.UnableAddItemException;
import javafish.clients.opc.exception.UnableRemoveGroupException;
import javafish.clients.opc.variant.Variant;
import javax.swing.JOptionPane;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYDatasetTableModel;

/**
 *
 * @author Flavio Mendes
 */
public class OPCRead extends javax.swing.JFrame {

    /**
     * Creates new form OPCRead
     */
    //Variaveis do Grafico
    TimeSeries serieSP, seriePV, serieMV;
    TimeSeriesCollection dataset;
    ChartPanel myChart;
    JFreeChart grafico;

    //Variaveis OPC
    JOpc opc;
    OpcItem tagSP, tagPV, tagMV, tagModo, tagKp, tagKi, tagKd;
    OpcGroup grupo, leituraOPC;

    //Variaveis de leitura
    Date data;
    double SP, PV, MV, modo, Kp, Ki, Kd, erro = 0, amp, eps, MVoper, K, Tau, D;
    ArrayList<Double> func;
    ArrayList<Double> y = new ArrayList<>(); //saidas (PV)
    ArrayList<Double> u = new ArrayList<>(); //entradas (MV)
    ArrayList<Integer> periodos = new ArrayList<>(); //guardar periodos
    int ciclos, cont, k; //variaveis de operação para o rele

    //Outras variaveis
    boolean leitura = false, flagRele = false, flagModo = false;
    double fator = 163.83;
    double fatorSintonia = 100;

    //Metodo construtor
    public OPCRead() {
        initComponents();
        IniciarGrafico();
        ciclos = 6;
    }

    //Função para mostrar o grafico na tela
    public void IniciarGrafico() {
        //Criar series
        serieSP = new TimeSeries("SP");
        seriePV = new TimeSeries("PV");
        serieMV = new TimeSeries("MV");

        //Iniciar coleção
        dataset = new TimeSeriesCollection();

        /*Adiconar series a coleção*/
        dataset.addSeries(serieSP);
        dataset.addSeries(seriePV);
        dataset.addSeries(serieMV);

        //Fabricar o grafico
        XYDataset dados = dataset;
        grafico = ChartFactory.createTimeSeriesChart("Dados OPC", "Tempo(s)", "Valores", dados);

        //Criar Painel
        myChart = new ChartPanel(grafico, true);
        myChart.setSize(panelGrafico.getWidth(), panelGrafico.getHeight());
        myChart.setVisible(true);
        panelGrafico.removeAll();
        panelGrafico.add(myChart);
        panelGrafico.repaint();

    }

    //Função para conectar ao servidor opc
    public void conectarOPC() {
        JOpc.coInitialize();
        //Criar Objeto Servidor
        opc = new JOpc(txtHost.getText(), txtServeOpc.getText(), "OPC1");

        tagSP = new OpcItem(txtTagSP.getText(), true, "");
        tagPV = new OpcItem(txtTagPV.getText(), true, "");
        tagMV = new OpcItem(txtTagMV.getText(), true, "");
        tagModo = new OpcItem(txtTagModo.getText(), true, "");
        tagKp = new OpcItem("[P_UNP]N7:16", true, "");
        tagKi = new OpcItem("[P_UNP]N7:17", true, "");
        tagKd = new OpcItem("[P_UNP]N7:18", true, "");

        //Criar grupo OPC
        grupo = new OpcGroup("G1", true, 100, 0.0f);

        //Adicionar itens ao grupo OPC
        grupo.addItem(tagSP);
        grupo.addItem(tagPV);
        grupo.addItem(tagMV);
        grupo.addItem(tagModo);
        grupo.addItem(tagKp);
        grupo.addItem(tagKi);
        grupo.addItem(tagKd);

        //Adicionar o grupo ao servidor OPC
        opc.addGroup(grupo);

        try {
            //Conectar e registrar grupos OPC
            opc.connect();
            opc.registerGroups();
            lbStatus.setText("Conectado!");
            lbStatus.setForeground(Color.BLUE);
        } catch (ConnectivityException | UnableAddGroupException | UnableAddItemException ex) {
            lbStatus.setText("Erro de conexão OPC!");
            lbStatus.setForeground(Color.RED);
            Logger.getLogger(OPCRead.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    //Função para desconectar do servidor
    public void Desconectar() {
        try {
            opc.unregisterGroups();
            lbStatus.setText("Não Conectado!");
            lbStatus.setForeground(Color.red);
        } catch (UnableRemoveGroupException ex) {
            lbStatus.setText("Erro ao desconectar");
            Logger.getLogger(OPCRead.class.getName()).log(Level.SEVERE, null, ex);
        }

        JOpc.coUninitialize();

    }

    //Função para fazer leitura das tags opc
    public void LeituraTagsOPC() {
        data = new Date();

        if (opc.ping()) {
            try {
                leituraOPC = opc.synchReadGroup(grupo);
            } catch (ComponentNotFoundException | SynchReadException ex) {
                JOptionPane.showMessageDialog(null, "Erro de leitura OPC!");
                Logger.getLogger(OPCRead.class.getName()).log(Level.SEVERE, null, ex);
            }

            SP = Double.parseDouble(leituraOPC.getItems().get(0).getValue().toString()) / fator;
            PV = Double.parseDouble(leituraOPC.getItems().get(1).getValue().toString()) / fator;
            MV = Double.parseDouble(leituraOPC.getItems().get(2).getValue().toString()) / fator;
            modo = Double.parseDouble(leituraOPC.getItems().get(3).getValue().toString());
            Kp = Double.parseDouble(leituraOPC.getItems().get(4).getValue().toString()) / fatorSintonia;
            Ki = Double.parseDouble(leituraOPC.getItems().get(5).getValue().toString()) / fatorSintonia;
            Kd = Double.parseDouble(leituraOPC.getItems().get(6).getValue().toString()) / fatorSintonia;

            //Atualizar botão para automatico
            if (modo > 0) {
                btnModo.setBackground(Color.red);
                btnModo.setText("Manual");
            } else {
                btnModo.setBackground(Color.green);
                btnModo.setText("Automatico");
            }
            //Atualizar campos de texto
            txtSP.setText(truncar(SP));
            txtPV.setText(truncar(PV));
            txtMV.setText(truncar(MV));
            txtKP.setText(truncar(Kp));
            txtKI.setText(truncar(Ki));
            txtKD.setText(truncar(Kd));

            //Atualizar Grafico
            serieSP.addOrUpdate(new Millisecond(data), SP);
            seriePV.addOrUpdate(new Millisecond(data), PV);
            serieMV.addOrUpdate(new Millisecond(data), MV);

            if (flagRele) {
                if (cont <= ciclos) {
                    rele();
                } else {
                    paraRele();
                    parametrosEstimados();

                }
            }
        }

    }

    //Função para truncar o valor decimal em duas casas
    public static String truncar(double valor) {
        DecimalFormat df = new DecimalFormat("#0.00");

        return df.format(valor);
    }

    //Função para escrever as tags opc
    public void escrever(OpcItem Tag, double valor) {
        Tag.setValue(new Variant(valor));
        try {
            opc.synchWriteItem(grupo, Tag);
        } catch (ComponentNotFoundException | SynchWriteException ex) {
            Logger.getLogger(OPCRead.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //Função para habilitar o rele
    public void rele() {
        erro = SP - PV;
        //Rele para baixo ou para cima somente no começo
        if (cont == 0) {
            if (erro >= 0) {
                escrever(tagMV, (MVoper + amp) * fator);
            } else {
                escrever(tagMV, (MVoper - amp) * fator);
            }
        } else if (erro >= eps) {
            escrever(tagMV, (MVoper + amp) * fator);
        } else if (erro <= -eps) {
            escrever(tagMV, (MVoper - amp) * fator);
        }

        //Armazenar saidas e entradas
        y.add(PV);
        u.add(MV);

        if (k > 1) {
            //Verifique se houve mundança no sinal
            if (Math.round(u.get(k)) != Math.round(u.get(k - 1))) {
                cont++; //houve mundança
                periodos.add(k);
            }
        }

        k++;
    }

    //Função de parar o rele
    public void paraRele() {
        //PID para automatico
        escrever(tagModo, 0);
        Esperar("20"); // esperar 20 ms
        flagRele = false;

    }

    //Função que faz o loop de leitura de tags opc e espera
    public void LoopLeitura() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (leitura) {
                    LeituraTagsOPC();
                    Esperar(cbTAmostra.getSelectedItem().toString());
                }
                if (!leitura) {
                    try {
                        finalize();
                    } catch (Throwable ex) {
                        Logger.getLogger(OPCRead.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }).start();
    }

    //Função delay, que pausa por um periodo de tempo
    public void Esperar(String tempo) {
        Long t = Long.parseLong(tempo);
        if (t > 0) {
            try {
                Thread.sleep(t);
            } catch (InterruptedException ex) {
                Logger.getLogger(OPCRead.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    //Função que mostra o modelo de primeira ordem com atraso de transporte
    public static ArrayList<Double> ModFOPDT(ArrayList<Integer> periodos, double amp, double eps, double ref, ArrayList<Double> y, ArrayList<Double> u, double Tamostragem) {
        ArrayList<Double> ParFOPDT = new ArrayList<>();
        double Tu, RefAux, Au, Ad, a, Ku, fase, k, tau, teta;
        int aux1, aux2;
        ParFOPDT.clear();

        //calculo do periodo total
        Tu = ((periodos.get(5) - periodos.get(4)) * Tamostragem) + ((periodos.get(4) - periodos.get(3)) * Tamostragem);
        aux1 = periodos.get(2);
        aux2 = periodos.get(4);
        double yi1[] = new double[aux2 - aux1 + 2];
        double yi2[] = new double[aux2 - aux1 + 2];
        double ui1[] = new double[aux2 - aux1 + 2];
        double ui2[] = new double[aux2 - aux1 + 2];
        double ti1[] = new double[aux2 - aux1 + 2];
        double ti2[] = new double[aux2 - aux1 + 2];

        RefAux = ref;
        for (int t = aux1; t <= aux2 - 1; t++)//pico de positivo
        {
            if (y.get(t - 1) >= RefAux) {
                RefAux = y.get(t - 1);
            }
        }
        Au = RefAux;//guardar pico de subida
        RefAux = ref;

        for (int t = aux1; t <= aux2 - 1; t++)//pico negativo
        {
            if (y.get(t - 1) <= RefAux) {
                RefAux = y.get(t - 1);
            }
        }
        Ad = RefAux;

        a = (Math.abs(Au) - Math.abs(Ad)) / 2;//amplitude de saída
        //função descritiva do relé
        Ku = (4 * amp) / (Math.PI * a);
        fase = Math.asin((eps / a)) * -1;//calcular defasagem da histerese
        //-----<Inicio do Calculo Ganho estático - Méetodo da Integral>---------------- 
        int i = 0;
        yi1[i] = 0;
        ui1[i] = 0;
        ti2[i] = 0;
        for (int t = aux1; t <= aux2; t++) {//laço para colher os dados de 1 periodos completo do teste
            yi1[i + 1] = y.get(t - 1);
            yi2[i] = y.get(t - 1);

            ui1[i + 1] = u.get(t - 1);
            ui2[i] = u.get(t - 1);

            ti1[i] = (i + 1) * Tamostragem;
            ti2[i + 1] = (i + 1) * Tamostragem;
            i = i + 1;
        }
        yi2[i] = 0;
        ui2[i] = 0;
        ti1[i] = 0;

        Matrix Yi1 = new Matrix(yi1, 1);
        Matrix Yi2 = new Matrix(yi2, 1);
        Matrix Ti1 = new Matrix(ti1, 1);
        Matrix Ti2 = new Matrix(ti2, 1);

        Yi1 = Yi1.plusEquals(Yi2);
        Ti1 = Ti1.minusEquals(Ti2);
        Yi1 = Yi1.arrayTimes(Ti1).times(0.5);

        double A1 = 0;
        for (int j = 1; j < Yi1.getColumnDimension() - 1; j++) {
            A1 = A1 + Yi1.get(0, j);
        }
        Matrix Ui1 = new Matrix(ui1, 1);
        Matrix Ui2 = new Matrix(ui2, 1);

        Ui1 = Ui1.plusEquals(Ui2);
        Ui1 = Ui1.arrayTimes(Ti1).times(0.5);
        //Ui1 = Ui1.times(0.5);

        double A2 = 0;
        for (int j = 1; j < Ui1.getColumnDimension() - 1; j++) {
            A2 = A2 + Ui1.get(0, j);
        }

        k = A1 / A2;//ganho estático
        double delta;
        if (Ku * k < 1) {
            delta = 1;
        } else {
            delta = Math.pow((Ku * k), 2) - 1;
        }

        //----<Fim do calculo ganho estático da planta>-------------------------------
        tau = (Tu / (2 * Math.PI)) * Math.sqrt(delta);
        teta = (Tu / (2 * Math.PI)) * (Math.PI - Math.atan((2 * Math.PI * tau) / Tu) + fase);

        ParFOPDT.add(0, k);
        ParFOPDT.add(1, tau);
        ParFOPDT.add(2, teta);

        return ParFOPDT;
    }

    //Função de estimar os parametros K, Tau, D e mostrar na tela
    public void parametrosEstimados() {

        func = ModFOPDT(periodos, amp, eps, SP, y, u, (Double.parseDouble(cbTAmostra.getSelectedItem().toString()) / 1000));
        K = func.get(0);
        txtK.setText(String.valueOf(truncar(K)));
        Tau = func.get(1);
        txtTau.setText(String.valueOf(truncar(Tau)));
        D = func.get(2);
        txtD.setText(String.valueOf(truncar(D)));

    }

    //Função de Sintonia PI
    public void sinPI() {

        func = ModFOPDT(periodos, amp, eps, SP, y, u, (Double.parseDouble(cbTAmostra.getSelectedItem().toString()) / 1000));
        K = func.get(0);
        Tau = func.get(1);
        D = func.get(2);
        double Kp, Ti, Td;
        String metodo = cbMetododeSitonia.getSelectedItem().toString();
        switch (metodo) { //Metodos de sitonia PI
            case "Ziegler-Nichols":
                if (radPI.isSelected() && (metodo == "Ziegler-Nichols")) {
                    Kp = 0.9 * (Tau / (K * D));
                    Ti = (3.33 * D) / 60;
                    Td = 0;

                    txtKp.setText(String.valueOf(truncar(Kp)));
                    txtTi.setText(String.valueOf(truncar(Ti)));
                    txtTd.setText(String.valueOf(truncar(Td)));
                }
            case "CHR":
                if (radPI.isSelected() && (metodo == "CHR")) {
                    Kp = 0.6 * (Tau / (K * D));
                    Ti = (4 * D) / 60;
                    Td = 0;

                    txtKp.setText(String.valueOf(truncar(Kp)));
                    txtTi.setText(String.valueOf(truncar(Ti)));
                    txtTd.setText(String.valueOf(truncar(Td)));
                }
            case "IAE":
                if (radPI.isSelected() && (metodo == "IAE")) {
                    Kp = (0.758 / K) * Math.pow((Tau / D), 0.861);
                    Ti = (Tau / (1.02 - 0.323 * (D / Tau))) / 60;
                    Td = 0;

                    txtKp.setText(String.valueOf(truncar(Kp)));
                    txtTi.setText(String.valueOf(truncar(Ti)));
                    txtTd.setText(String.valueOf(truncar(Td)));
                }
            case "ITAE":
                if (radPI.isSelected() && (metodo == "ITAE")) {
                    Kp = (0.586 / K) * Math.pow((Tau / D), 0.916);
                    Ti = (Tau / (1.03 - 0.165 * (D / Tau))) / 60;
                    Td = 0;

                    txtKp.setText(String.valueOf(truncar(Kp)));
                    txtTi.setText(String.valueOf(truncar(Ti)));
                    txtTd.setText(String.valueOf(truncar(Td)));
                }
        }

    }

    //Função de Sintonia PID
    public void sinPID() {
        func = ModFOPDT(periodos, amp, eps, SP, y, u, (Double.parseDouble(cbTAmostra.getSelectedItem().toString()) / 1000));
        K = func.get(0);
        Tau = func.get(1);
        D = func.get(2);
        double Kp, Ti, Td;
        String metodo = cbMetododeSitonia.getSelectedItem().toString();
        switch (metodo) { //Metodos de sitonia PID
            case "Ziegler-Nichols":
                if (radPID.isSelected() && (metodo == "Ziegler-Nichols")) {
                    Kp = 1.2 * (Tau / (K * D));
                    Ti = (2 * D) / 60;
                    Td = (0.5 * D) / 60;

                    txtKp.setText(String.valueOf(truncar(Kp)));
                    txtTi.setText(String.valueOf(truncar(Ti)));
                    txtTd.setText(String.valueOf(truncar(Td)));
                }
            case "CHR":
                if (radPID.isSelected() && (metodo == "CHR")) {
                    Kp = 0.95 * (Tau / (K * D));
                    Ti = (2.375 * D) / 60;
                    Td = (0.421 * D) / 60;

                    txtKp.setText(String.valueOf(truncar(Kp)));
                    txtTi.setText(String.valueOf(truncar(Ti)));
                    txtTd.setText(String.valueOf(truncar(Td)));
                }
            case "IAE":
                if (radPID.isSelected() && (metodo == "IAE")) {
                    Kp = (1.086 / K) * Math.pow((Tau / D), 0.869);
                    Ti = (Tau / (0.740 - 0.130 * (D / Tau))) / 60;
                    Td = (0.348 * Tau * Math.pow((D / Tau), 0.914)) / 60;

                    txtKp.setText(String.valueOf(truncar(Kp)));
                    txtTi.setText(String.valueOf(truncar(Ti)));
                    txtTd.setText(String.valueOf(truncar(Td)));
                }
            case "ITAE":
                if (radPID.isSelected() && (metodo == "ITAE")) {
                    Kp = (0.965 / K) * Math.pow((Tau / D), 0.850);
                    Ti = (Tau / (0.796 - 0.147 * (D / Tau))) / 60;
                    Td = (0.308 * Tau * Math.pow((D / Tau), 0.929)) / 60;

                    txtKp.setText(String.valueOf(truncar(Kp)));
                    txtTi.setText(String.valueOf(truncar(Ti)));
                    txtTd.setText(String.valueOf(truncar(Td)));
                }
        }

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        buttonGroup3 = new javax.swing.ButtonGroup();
        buttonGroup4 = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        txtServeOpc = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        txtHost = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        lbStatus = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        txtTagSP = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        txtTagPV = new javax.swing.JTextField();
        btnConectar = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        txtTagMV = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        txtTagModo = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        cbTAmostra = new javax.swing.JComboBox<String>();
        btnIniciar = new javax.swing.JButton();
        btnModo = new javax.swing.JButton();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        txtSP = new javax.swing.JTextField();
        txtPV = new javax.swing.JTextField();
        txtMV = new javax.swing.JTextField();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        txtKP = new javax.swing.JTextField();
        txtKI = new javax.swing.JTextField();
        txtKD = new javax.swing.JTextField();
        panelGrafico = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        btnIniciaRele = new javax.swing.JButton();
        btnPararRele = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        txtAmp = new javax.swing.JTextField();
        txtEps = new javax.swing.JTextField();
        jPanel3 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        txtK = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        txtTau = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        txtD = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        radPI = new javax.swing.JRadioButton();
        radPID = new javax.swing.JRadioButton();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        txtKp = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        txtTi = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        txtTd = new javax.swing.JTextField();
        cbMetododeSitonia = new javax.swing.JComboBox<String>();
        btnSitonia = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jLabel18 = new javax.swing.JLabel();
        txtSp = new javax.swing.JTextField();
        btnSetPoint = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel1.setText("Servidor OPC");

        txtServeOpc.setText("RSLinx Remote OPC Server");

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel2.setText("Host");

        txtHost.setText("localhost");

        jLabel3.setText("Status:");

        lbStatus.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        lbStatus.setForeground(new java.awt.Color(255, 0, 0));
        lbStatus.setText("Não Conectado!");

        jLabel5.setText("TagSP");

        txtTagSP.setText("[P_UNP]N7:19");

        jLabel6.setText("TagPV");

        txtTagPV.setText("[P_UNP]N7:59");

        btnConectar.setText("Conectar");
        btnConectar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConectarActionPerformed(evt);
            }
        });

        jLabel7.setText("TagMV");

        txtTagMV.setText("[P_UNP]N7:20");

        jLabel8.setText("TagModo");

        txtTagModo.setText("[P_UNP]B19:0/2");

        jLabel9.setText("Tempo de Amostragem (ms) :");

        cbTAmostra.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "100", "200", "300", "500", "1000", "2000", "3000" }));
        cbTAmostra.setSelectedIndex(3);

        btnIniciar.setText("Read On");
        btnIniciar.setEnabled(false);
        btnIniciar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnIniciarActionPerformed(evt);
            }
        });

        btnModo.setText("Automatico");
        btnModo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnModoActionPerformed(evt);
            }
        });

        jLabel19.setText("SP:");

        jLabel20.setText("PV:");

        jLabel21.setText("MV:");

        jLabel22.setText("KP:");

        jLabel23.setText("KI:");

        jLabel24.setText("KD:");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addComponent(jLabel6)
                    .addComponent(jLabel8))
                .addGap(20, 20, 20)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(txtTagSP)
                    .addComponent(txtTagPV)
                    .addComponent(txtTagMV, javax.swing.GroupLayout.PREFERRED_SIZE, 231, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtTagModo, javax.swing.GroupLayout.PREFERRED_SIZE, 231, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 23, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jLabel21))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel19, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel20, javax.swing.GroupLayout.Alignment.TRAILING))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(txtPV)
                    .addComponent(txtMV)
                    .addComponent(txtSP, javax.swing.GroupLayout.Alignment.LEADING))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel23)
                    .addComponent(jLabel22)
                    .addComponent(jLabel24))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(txtKP)
                    .addComponent(txtKI)
                    .addComponent(txtKD, javax.swing.GroupLayout.PREFERRED_SIZE, 114, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(32, 32, 32)
                .addComponent(txtHost)
                .addGap(23, 23, 23))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(32, 32, 32)
                .addComponent(txtServeOpc)
                .addGap(23, 23, 23))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jLabel9))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(cbTAmostra, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(btnIniciar, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnModo, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(32, 32, 32)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel1)
                            .addComponent(btnConectar, javax.swing.GroupLayout.PREFERRED_SIZE, 153, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(lbStatus))
                            .addComponent(jLabel7))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtServeOpc, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtHost, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnConectar)
                .addGap(20, 20, 20)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(lbStatus))
                .addGap(31, 31, 31)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtTagSP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtTagPV, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(txtTagMV, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(txtTagModo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(26, 26, 26)
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(cbTAmostra, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnIniciar, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnModo, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtSP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel19))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtPV, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel20))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtMV, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel21)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel22)
                            .addComponent(txtKP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel23)
                            .addComponent(txtKI, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel24)
                            .addComponent(txtKD, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        panelGrafico.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        javax.swing.GroupLayout panelGraficoLayout = new javax.swing.GroupLayout(panelGrafico);
        panelGrafico.setLayout(panelGraficoLayout);
        panelGraficoLayout.setHorizontalGroup(
            panelGraficoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 645, Short.MAX_VALUE)
        );
        panelGraficoLayout.setVerticalGroup(
            panelGraficoLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Método do Relé"));

        btnIniciaRele.setText("Iniciar");
        btnIniciaRele.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnIniciaReleActionPerformed(evt);
            }
        });

        btnPararRele.setText("Parar");
        btnPararRele.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPararReleActionPerformed(evt);
            }
        });

        jLabel4.setText("Amplitude do Relé");

        jLabel10.setText("Histerese (eps)");

        txtAmp.setText("10");

        txtEps.setText("0.1");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(0, 25, Short.MAX_VALUE)
                        .addComponent(btnIniciaRele, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnPararRele, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addComponent(jLabel10))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtAmp)
                            .addComponent(txtEps))))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(btnPararRele, javax.swing.GroupLayout.DEFAULT_SIZE, 52, Short.MAX_VALUE)
                    .addComponent(btnIniciaRele, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(txtAmp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(txtEps, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(19, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Paramêtros Estimados"));

        jLabel11.setText("K");

        jLabel12.setText("Tau");

        jLabel13.setText("D");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtK, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel12)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtTau, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtD)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(txtK, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel12)
                    .addComponent(txtTau, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel13)
                    .addComponent(txtD, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Sitonia"));

        buttonGroup1.add(radPI);
        radPI.setText("PI");
        radPI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radPIActionPerformed(evt);
            }
        });

        buttonGroup1.add(radPID);
        radPID.setText("PID");
        radPID.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radPIDActionPerformed(evt);
            }
        });

        jLabel14.setText("Método");

        jLabel15.setText("Kp");

        jLabel16.setText("Ti");

        jLabel17.setText("Td");

        cbMetododeSitonia.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Ziegler-Nichols", "CHR", "IAE", "ITAE" }));

        btnSitonia.setText("Enviar Sitonia");
        btnSitonia.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSitoniaActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(radPI)
                        .addGap(18, 18, 18)
                        .addComponent(radPID))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel14)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(cbMetododeSitonia, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(btnSitonia, javax.swing.GroupLayout.PREFERRED_SIZE, 122, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel4Layout.createSequentialGroup()
                                .addComponent(jLabel15)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtKp, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel16)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(txtTi, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel17)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtTd, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radPI)
                    .addComponent(radPID))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel14)
                    .addComponent(cbMetododeSitonia, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15)
                    .addComponent(txtKp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel16)
                    .addComponent(txtTi, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel17)
                    .addComponent(txtTd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(btnSitonia)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Setpoint"));

        jLabel18.setText("Sp");

        btnSetPoint.setText("Enviar Setpoint");
        btnSetPoint.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSetPointActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel18)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(txtSp)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnSetPoint, javax.swing.GroupLayout.PREFERRED_SIZE, 122, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(81, 81, 81))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel18)
                    .addComponent(txtSp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(btnSetPoint)
                .addContainerGap(18, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelGrafico, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(1, 1, 1)
                        .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 10, Short.MAX_VALUE))
                    .addComponent(panelGrafico, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnConectarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConectarActionPerformed
        // TODO add your handling code here:
        //btnConectar.getText()=="Conectar"
        if ("Conectar".equals(btnConectar.getText())) {
            conectarOPC();
            btnIniciar.setEnabled(true);
            btnConectar.setText("Desconectar");
        } else {
            Desconectar();
            btnIniciar.setEnabled(false);
            btnConectar.setText("Conectar");
        }

    }//GEN-LAST:event_btnConectarActionPerformed

    private void btnIniciarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnIniciarActionPerformed
        // TODO add your handling code here:
        if (!leitura) {
            leitura = true;
            btnIniciar.setText("Read Off");
            btnConectar.setEnabled(false);
            LoopLeitura();
        } else {
            btnIniciar.setText("Read On");
            btnConectar.setEnabled(true);
            leitura = false;
        }

    }//GEN-LAST:event_btnIniciarActionPerformed

    private void btnIniciaReleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnIniciaReleActionPerformed
        // TODO add your handling code here:
        // Colocar o PID em Manual
        escrever(tagModo, 1);
        Esperar("20"); // esperar 20 ms

        //Inicializar variaveis rele
        k = 0;
        cont = 0;
        y.clear();
        u.clear();

        //Ler a posição atual do sinal de controle
        MVoper = MV;

        //Pegar amplitude e histerese do rele
        amp = Double.parseDouble(txtAmp.getText());
        eps = Double.parseDouble(txtEps.getText());

        //Flag do rele para true
        flagRele = true;
    }//GEN-LAST:event_btnIniciaReleActionPerformed

    private void btnPararReleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPararReleActionPerformed
        // TODO add your handling code here:
        // PID para automatico
        paraRele();
    }//GEN-LAST:event_btnPararReleActionPerformed

    private void radPIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radPIActionPerformed
        // TODO add your handling code here:
        sinPI();
    }//GEN-LAST:event_radPIActionPerformed

    private void radPIDActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_radPIDActionPerformed
        // TODO add your handling code here:
        sinPID();
    }//GEN-LAST:event_radPIDActionPerformed

    private void btnSitoniaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSitoniaActionPerformed
        // TODO add your handling code here:
        double kp, ti, td;
        kp = Double.parseDouble(txtKp.getText().replace(",", "."));
        ti = Double.parseDouble(txtTi.getText().replace(",", "."));
        td = Double.parseDouble(txtTd.getText().replace(",", "."));
        escrever(tagKp, (kp * fatorSintonia));
        Esperar("20");
        escrever(tagKi, (ti * fatorSintonia));
        Esperar("20");
        escrever(tagKd, (td * fatorSintonia));
        Esperar("20");
    }//GEN-LAST:event_btnSitoniaActionPerformed

    private void btnSetPointActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSetPointActionPerformed
        // TODO add your handling code here:
        double valor = Double.parseDouble(txtSp.getText());
        escrever(tagSP, valor * fator);
        Esperar("20");

    }//GEN-LAST:event_btnSetPointActionPerformed

    private void btnModoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnModoActionPerformed
        // TODO add your handling code here:
        if (flagModo) {
            flagModo = false;
            escrever(tagModo, 1);
            btnModo.setText("Manual");
            btnModo.setBackground(Color.RED);
        } else {
            flagModo = true;
            escrever(tagModo, 0);
            btnModo.setText("Automatico");
            btnModo.setBackground(Color.GREEN);
        }
    }//GEN-LAST:event_btnModoActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(OPCRead.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(OPCRead.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(OPCRead.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(OPCRead.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new OPCRead().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnConectar;
    private javax.swing.JButton btnIniciaRele;
    private javax.swing.JButton btnIniciar;
    private javax.swing.JButton btnModo;
    private javax.swing.JButton btnPararRele;
    private javax.swing.JButton btnSetPoint;
    private javax.swing.JButton btnSitonia;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    private javax.swing.ButtonGroup buttonGroup4;
    private javax.swing.JComboBox<String> cbMetododeSitonia;
    private javax.swing.JComboBox<String> cbTAmostra;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JLabel lbStatus;
    private javax.swing.JPanel panelGrafico;
    private javax.swing.JRadioButton radPI;
    private javax.swing.JRadioButton radPID;
    private javax.swing.JTextField txtAmp;
    private javax.swing.JTextField txtD;
    private javax.swing.JTextField txtEps;
    private javax.swing.JTextField txtHost;
    private javax.swing.JTextField txtK;
    private javax.swing.JTextField txtKD;
    private javax.swing.JTextField txtKI;
    private javax.swing.JTextField txtKP;
    private javax.swing.JTextField txtKp;
    private javax.swing.JTextField txtMV;
    private javax.swing.JTextField txtPV;
    private javax.swing.JTextField txtSP;
    private javax.swing.JTextField txtServeOpc;
    private javax.swing.JTextField txtSp;
    private javax.swing.JTextField txtTagMV;
    private javax.swing.JTextField txtTagModo;
    private javax.swing.JTextField txtTagPV;
    private javax.swing.JTextField txtTagSP;
    private javax.swing.JTextField txtTau;
    private javax.swing.JTextField txtTd;
    private javax.swing.JTextField txtTi;
    // End of variables declaration//GEN-END:variables
}
