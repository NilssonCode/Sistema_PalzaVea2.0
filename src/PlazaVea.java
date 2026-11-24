import javax.swing.*; //crear ventana, botones, etiquetas y formularios
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*; //trabajar colores, fuente y diseños
import java.awt.event.*;
import java.awt.image.BufferedImage; //manipular las imagenes
import java.io.*; //entrada y salida
import java.nio.charset.StandardCharsets;
import java.nio.file.*; //leer escribir y localizar
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List; //lista de elementos
//► CLASE PRINCIPAL
public class PlazaVea extends JFrame { //Hereda de la clase swing
    private static final Color ROJO = new Color(201,29,48), FONDO = new Color(246,247,250), OSCURO = new Color(43,48,57); //RGB
    private static final Font FUENTE = new Font("SansSerif",Font.PLAIN,13); //PLAIN: normal, BOLD:negrita
    // Rutas de los archivos persistentes y carpeta de boletas.
    private final Path datos = Paths.get("datos_plazvea"), archivoUsuarios=datos.resolve("usuarios.txt"), archivoPedidos=datos.resolve("pedidos.txt"), carpetaBoletas=datos.resolve("boletas");
    // Lista dinámica que contiene los productos y cuentas.
    private final List<Producto> catalogo = new ArrayList<>();
    private final List<Usuario> usuarios = new ArrayList<>();
    // Mapa: clave = ID del producto; valor = cantidad en el carrito.
    private final LinkedHashMap<Integer,Integer> carrito = new LinkedHashMap<>();
    private final List<Pedido> pedidos = new ArrayList<>(); //Lista de pedidos cargados en ejecución
    // Contenedor principal del sistema
    private final JPanel panelPrincipal = new JPanel(new BorderLayout());
    // Referencia a la cuenta que inició sesión; null significa que no hay sesión.
    private Usuario usuarioActual;
    private String modalidadEntrega="DOMICILIO", categoriaSeleccionada="Todas";
    private String busqueda="";
    private String filtroPedidosActivo=null;
    private int contadorPedidos=1;
    // Ejecuta la revisión de entregas cada 1000 milisegundos.
    private final javax.swing.Timer temporizadorEntrega = new javax.swing.Timer(1000, evento -> actualizarEntregas());
    // Arreglo unidimensional con las diez categorías.
    private static final String[] CATEGORIAS={"Supermercado","Frutas y verduras","Carnes y pescados","Bebidas","Limpieza","Cuidado personal","Tecnologia","Electrohogar","Hogar y bazar","Moda"};
    // Clase producto: identificador, nombre, categoría y precio.
    private static class Producto {
        int id;
        String nombre,categoria;
        double precio;
        Producto(int id,String numeroAux,String componenteAux,double dato) { //constructor
            this.id=id;
            nombre=numeroAux;
            categoria=componenteAux;
            precio=dato;
        }
    }
    // Clase Usuario: datos personales, puntos y saldo.
    private static class Usuario {
        String nombre,correo,contrasena,direccion,telefono;
        int puntos;
        double saldo;
        Usuario(String numeroAux,String evento,String dato,String auxiliar,String datoAux,int puntosIniciales) {
            nombre=numeroAux;
            correo=evento;
            contrasena=dato;
            direccion=auxiliar;
            telefono=datoAux;
            puntos=puntosIniciales;
            saldo=0;
        }
    }
    // Clase pedido: datos, estado, importe y momento de creación.
    private static class Pedido {
        String id,correo,fecha,estado,elementos,pago,direccion;
        double total;
        long momentoCreacion;
        Pedido(String indice,String evento,String datoFecha,String texto,String detalle,String medioPago,String auxiliar,double datoAux) {
            id=indice;
            correo=evento;
            fecha=datoFecha;
            estado=texto;
            elementos=detalle;
            pago=medioPago;
            direccion=auxiliar;
            total=datoAux;
            momentoCreacion=System.currentTimeMillis();
        }
    }

    // ----------------------------------------------------------------------------------
    // MÉTODO: PlazaVea
    // FUNCIÓN: Configura la ventana, recupera los datos guardados e inicia el sistema.
    // ----------------------------------------------------------------------------------
    public PlazaVea() {
        super("PLAZA VEA 2.0 | Supermercado"); //llama al constructor de la clase padre
        // Finaliza el programa al cerrar la ventana.
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1250,790);
        setMinimumSize(new Dimension(940,630));
        setLocationRelativeTo(null);
        // Establece el panel que contiene toda la interfaz.
        setContentPane(panelPrincipal);//establece el panle principal como contenedor principal
        panelPrincipal.setBackground(FONDO);
        // Inicializa el catálogo antes de mostrar la primera pantalla.
        cargarCatalogo();
        cargarUsuarios();
        cargarPedidos();
        // Activa la revisión periódica de pedidos.
        temporizadorEntrega.start();
        mostrarInicioSesion();
        setVisible(true);
    }

    // -------------------------------------------------------------------------------
    // MÉTODO: crearBoton
    // FUNCIÓN: Crea un botón rojo y vincula la acción que se ejecutará al pulsarlo.
    // -------------------------------------------------------------------------------
    private JButton crearBoton(String texto,Runnable accion) {
        JButton boton=new JButton(texto);
        boton.setBackground(ROJO); //fondo
        boton.setForeground(Color.WHITE); //texto
        boton.setFont(new Font("SansSerif",Font.BOLD,12));
        boton.setFocusPainted(false); //quitar el foco que aparee en el borde
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); //cambio del cursor
        boton.setBorder(new EmptyBorder(11,15,11,15));
        boton.addActionListener(evento->accion.run()); //accion al presionar el boton
        return boton;
    }

    // ----------------------------------------------------------------------------
    // MÉTODO: crearBotonSecundario
    // FUNCIÓN: Reutiliza el botón principal y cambia su apariencia a blanca.
    // ----------------------------------------------------------------------------
    private JButton crearBotonSecundario(String texto,Runnable accion) {
        JButton boton=crearBoton(texto,accion);
        boton.setBackground(Color.WHITE); //fondo
        boton.setForeground(OSCURO); //texto
        boton.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(222,224,230)), //borde exterior
                new EmptyBorder(10,13,10,13))); //borde interior
        return boton;
    }

    // -----------------------------------------------------------------------
    // MÉTODO: crearPanel
    // FUNCIÓN: Crea un contenedor blanco para agrupar componentes gráficos.
    // -----------------------------------------------------------------------
    private JPanel crearPanel() {
        JPanel dato=new JPanel(); //objeto de tipo JPanel
        dato.setBackground(Color.WHITE); //fondo
        return dato;
    }

    // ----------------------------------------------------------------------
    // MÉTODO: crearEtiqueta
    // FUNCIÓN: Crea un texto con tamaño, color y estilo configurables.
    // ----------------------------------------------------------------------
    private JLabel crearEtiqueta(String texto,int tamano,boolean negrita) {
        JLabel etiqueta=new JLabel(texto); //objeto etiqueta
        etiqueta.setFont(new Font("SansSerif",negrita?Font.BOLD:Font.PLAIN,tamano));
        etiqueta.setForeground(OSCURO); //texto
        return etiqueta;
    }

    // ---------------------------------------------------------------------
    // MÉTODO: crearPagina
    // FUNCIÓN: Construye un panel con título y espacio para el contenido.
    // ---------------------------------------------------------------------
    private JPanel crearPagina(String titulo) {
        JPanel dato=new JPanel(new BorderLayout(12,12)); //div 5 partes
        dato.setBackground(FONDO);
        dato.setBorder(new EmptyBorder(20,25,20,25));
        dato.add(crearEtiqueta(titulo,25,true),BorderLayout.NORTH);
        return dato;
    }

    // -----------------------------------------------------------------------------------
    // MÉTODO: mostrarPantalla
    // FUNCIÓN: Reemplaza los componentes visibles y muestra el menú cuando corresponde.
    // -----------------------------------------------------------------------------------
    private void mostrarPantalla(JComponent componenteAux,boolean navegacion) { //recibe un JPanel (pantalla completa - muestra c/s navegacion
        //si saliste de la pantalla de pedidos, se reinicia el filtro de búsqueda.
        if(!Boolean.TRUE.equals(componenteAux.getClientProperty("ordersView")))filtroPedidosActivo=null;
        panelPrincipal.removeAll(); //retira los componentes gráficos del contenedor
        if(navegacion)panelPrincipal.add(crearEncabezado(),BorderLayout.NORTH); //construye el encabezado
        panelPrincipal.add(componenteAux,BorderLayout.CENTER);
        panelPrincipal.revalidate(); //vuelve a validar y calcular cada que elimin o agreg componentes a un panel
        panelPrincipal.repaint(); //vuelve a pintar el componente
    }

    // --------------------------------------------------------------------------
    // MÉTODO: crearEncabezado
    // FUNCIÓN: Construye el menú de navegación y muestra el usuario y sus puntos.
    // ----------------------------------------------------------------------------
    private JPanel crearEncabezado() {
        JPanel altoAux=crearPanel();
        altoAux.setLayout(new BorderLayout(15,5)); //ubicar elemntos por zonas
        altoAux.setBorder(new EmptyBorder(12,20,12,20)); //margen
        JLabel logo=crearEtiqueta("plazaVEA",29,true); //llamaod a la función etiqueta
        logo.setForeground(ROJO); //cambio de color texto
        altoAux.add(logo,BorderLayout.WEST);
        JPanel menu=crearPanel();
        menu.setLayout(new FlowLayout(FlowLayout.RIGHT,7,0)); //ordena los botones en una sola fila pegado a la derecha
        menu.add(crearBotonSecundario("Productos",()->mostrarCatalogo("Todas",""))); //expresion lambda (accion rapida)
        menu.add(crearBotonSecundario("Carrito ("+carrito.values().stream().mapToInt(Integer::intValue).sum()+")",this::mostrarCarrito)); //suam cant productos
        menu.add(crearBotonSecundario("Mis pedidos",()->mostrarPedidos("Todos")));
        menu.add(crearBotonSecundario("Mi perfil",this::mostrarPerfil));
        altoAux.add(menu,BorderLayout.CENTER);
        //aplicando operador ternario (if-else)
        JLabel user=crearEtiqueta(usuarioActual==null?"":usuarioActual.nombre+" | "+usuarioActual.puntos+" pts",12,false);
        altoAux.add(user,BorderLayout.SOUTH);
        return altoAux;
    }

    // -----------------------------------------------------------------
    // MÉTODO: centrar
    // FUNCIÓN: Coloca un componente en el centro de un panel exterior.
    // -----------------------------------------------------------------
    private JPanel centrar(JComponent contenido) {
        JPanel exterior=new JPanel(new GridBagLayout()); //administrador de diseño más flexible y potente(similar a combinar celdas en Excel)
        exterior.setBackground(FONDO);
        exterior.add(contenido);
        return exterior;
    }

    // ---------------------------------------------------------------------
    // MÉTODO: crearCampo
    // FUNCIÓN: Crea un campo de texto con valor inicial y tamaño definido.
    // ---------------------------------------------------------------------
    private JTextField crearCampo(String inicial) { //caja de texto
        JTextField campoTexto=new JTextField(inicial,21); //ancho base de 21 columnas de caracteres.
        campoTexto.setFont(FUENTE);
        campoTexto.setPreferredSize(new Dimension(280,32)); //tamaño preferido del campo
        return campoTexto;
    }

    // ----------------------------------------------------------------------------------
    // MÉTODO: mostrarInicioSesion
    // FUNCIÓN: Solicita correo y contraseña y comprueba las credenciales registradas.
    // ----------------------------------------------------------------------------------
    private void mostrarInicioSesion() {
        JPanel contenedor=crearPanel();
        contenedor.setLayout(new BoxLayout(contenedor,BoxLayout.Y_AXIS)); //organizamos los componenetes segun el eje
        contenedor.setBorder(new EmptyBorder(35,45,35,45));
        JLabel titulo=crearEtiqueta("Bienvenido a PLAZA VEA",27,true);
        titulo.setForeground(ROJO);
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        contenedor.add(titulo);
        contenedor.add(Box.createVerticalStrut(16)); //agrega un espacio, como un separador o margen vertical
        JLabel subtitulo=crearEtiqueta("Inicia sesión para continuar",14,false);
        subtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        contenedor.add(subtitulo);
        contenedor.add(Box.createVerticalStrut(22));
        // creación delos campos de correo y contraseña
        JTextField correo=new JTextField(); //caja vacia lista para escribir
        JPasswordField clave=new JPasswordField(); //caja especial para contraseñas
        Dimension tamano=new Dimension(300,36);

        for(JComponent componenteAux:new JComponent[]{crearEtiqueta("Correo electrónico",13,true),correo,crearEtiqueta("Contraseña",13,true),clave}) {
            componenteAux.setAlignmentX(Component.CENTER_ALIGNMENT);
            if(componenteAux instanceof JTextField) { //¿El elemento que estoy revisando ahorita es una caja para escribir texto?
                componenteAux.setPreferredSize(tamano);
                componenteAux.setMaximumSize(tamano);
                componenteAux.setMinimumSize(tamano);
            }
            //agregamos el componente al formulario y coloca una separación vertical de 10pix
            contenedor.add(componenteAux);
            contenedor.add(Box.createVerticalStrut(10));
        }
        // La expresión lambda se ejecuta cuando se pulsa iniciar sesión.
        JButton acceso=crearBoton("INICIAR SESIÓN",()-> {
            String evento=correo.getText().trim(), claveIngresada=new String(clave.getPassword());
            // Validar acceso al usuario. (Revisa cada cuenta guardada en la lista usuarios una por una.)
            for(Usuario usuario:usuarios) if(usuario.correo.equalsIgnoreCase(evento) && usuario.contrasena.equals(claveIngresada)) {
                // Conserva la referencia a la cuenta autenticada o recién registrada.
                usuarioActual=usuario;
                mostrarEntrega();
                return;
            }
            mensaje("Correo o contraseña incorrectos.");
        });
        acceso.setAlignmentX(Component.CENTER_ALIGNMENT);
        contenedor.add(acceso);
        contenedor.add(Box.createVerticalStrut(12)); //agregar espacio entre botones
        JButton registro=crearBotonSecundario("CREAR CUENTA",this::mostrarRegistro); //abra la pantalla mostrarRegistro
        registro.setAlignmentX(Component.CENTER_ALIGNMENT);
        contenedor.add(registro);
        mostrarPantalla(centrar(contenedor),false); //toma el contenedor y ubica justo al medio - no muestra encabezado
    }

    // ---------------------------------------------------------------------------
    // MÉTODO: mostrarRegistro
    // FUNCIÓN: Valida los datos, crea una cuenta y la guarda en el archivo TXT.
    // ---------------------------------------------------------------------------
    private void mostrarRegistro() {
        JPanel contenedor=crearPanel();
        contenedor.setLayout(new GridLayout(0,2,12,13)); //organiza los elemntos (como una tabla)
        contenedor.setBorder(new EmptyBorder(30,35,30,35));
        //crear los campos
        JTextField nombre=crearCampo(""),correo=crearCampo(""),direccion=crearCampo(""),telefono=crearCampo("");
        //crear campo dfe contraseña con 21 columnas de referencia
        JPasswordField claveIngresada=new JPasswordField(21);
        //agregamos las etiquetas y los campos
        contenedor.add(crearEtiqueta("Nombre completo",13,true));
        contenedor.add(nombre);
        contenedor.add(crearEtiqueta("Correo",13,true));
        contenedor.add(correo);
        contenedor.add(crearEtiqueta("Contrasena",13,true));
        contenedor.add(claveIngresada);
        contenedor.add(crearEtiqueta("Telefono",13,true));
        contenedor.add(telefono);
        contenedor.add(crearEtiqueta("Direccion",13,true));
        contenedor.add(direccion);

        contenedor.add(crearBotonSecundario("Volver",this::mostrarInicioSesion)); //ejecuta al hacer click
        contenedor.add(crearBoton("CREAR CUENTA",()-> {
            String numeroAux=nombre.getText().trim();
            String evento=correo.getText().trim().toLowerCase(Locale.ROOT); //mayuscula a minuscula
            String dato=new String(claveIngresada.getPassword());
            String auxiliar=direccion.getText().trim();
            String datoAux=telefono.getText().trim();
            // |!evento.matches|Valida nombre, estructura del correo y longitud mínima de la clave.
            if(numeroAux.isEmpty()||!evento.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")||dato.length()<4) {
                mensaje("Ingrese nombre, correo valido y clave de al menos 4 caracteres.");
                return;
            }
            if((numeroAux+evento+auxiliar+datoAux).contains("|")) { //comprobar si el texto contiene "|"
                mensaje("No utilice el caracter | en los datos.");
                return;
            }
            //validación de correo duplicado
            for(Usuario usuario:usuarios) if(usuario.correo.equals(evento)) {
                mensaje("Ese correo ya existe.");
                return;
            }
            // Crea una cuenta con cero puntos iniciales.
            Usuario usuario=new Usuario(numeroAux,evento,dato,auxiliar,datoAux,0);
            usuarios.add(usuario);
            try {
                guardarUsuarios();
                // Conserva la referencia a la cuenta autenticada o recién registrada.
                usuarioActual=usuario;
                mostrarEntrega();
            } catch(IOException excepcion) {
                usuarios.remove(usuario);
                mostrarError(excepcion);
            }
        }));
        JPanel envoltura=crearPagina("Crear cuenta");
        envoltura.add(centrar(contenedor),BorderLayout.CENTER);
        mostrarPantalla(envoltura,false);
    }

    // ---------------------------------------------------------------------
    // MÉTODO: mostrarEntrega
    // FUNCIÓN: Permite elegir domicilio o recojo y guardar la dirección.
    // ---------------------------------------------------------------------
    private void mostrarEntrega() {
        JPanel dato=crearPagina("Confirma la direccion de entrega");
        JPanel contenedor=crearPanel();
        contenedor.setLayout(new GridLayout(0,1,10,10));
        contenedor.setBorder(new EmptyBorder(25,30,25,30));
        //crear  opciones de seleccion para la modalidad de entrega
        JRadioButton domicilio=new JRadioButton("DESPACHO A DOMICILIO",modalidadEntrega.equals("DOMICILIO")),recojo=new JRadioButton("RECOJO EN TIENDA",modalidadEntrega.equals("RECOJO"));
        domicilio.setBackground(Color.WHITE); // fondo
        recojo.setBackground(Color.WHITE);
        //crear un grupo de botones de selccion
        ButtonGroup grupo=new ButtonGroup();
        grupo.add(domicilio);
        grupo.add(recojo);

        JTextField direccion=crearCampo(usuarioActual.direccion); //linea de texto
        contenedor.add(crearEtiqueta("Selecciona como recibiras tu compra",17,true));
        contenedor.add(domicilio);
        contenedor.add(recojo);
        contenedor.add(crearEtiqueta("Direccion de entrega / referencia",13,true));
        contenedor.add(direccion);
        contenedor.add(crearBoton("CONFIRMAR Y VER PRODUCTOS",()-> {
            String auxiliar=direccion.getText().trim();
            //validación de direcion
            if(domicilio.isSelected() && auxiliar.isEmpty()) {
                mensaje("Ingresa tu direccion de entrega.");
                return;
            }
            modalidadEntrega=domicilio.isSelected()?"DOMICILIO":"RECOJO";
            usuarioActual.direccion=auxiliar; //guarda la direccion del usuario
            try {
                guardarUsuarios();
                mostrarCatalogo("Todas","");
            } catch(IOException excepcion) {
                mostrarError(excepcion);
            }
        }));
        dato.add(centrar(contenedor),BorderLayout.CENTER);
        mostrarPantalla(dato,false);
    }

    // ------------------------------------------------------------------------------------------------
    // MÉTODO: cargarCatalogo
    // FUNCIÓN: Recorre la matriz de productos, calcula precios y crea los objetos del catálogo.
    // ---------------------------------------------------------------------------------------------------
    private void cargarCatalogo() {
        // Matriz bidimensional: cada fila corresponde a una categoría y contiene 20 productos.
        String[][] nombres={
                {"Arroz COSTEÑO extra 5kg","Azucar CARTAVIO rubia 1kg","Aceite PRIMOR vegetal 1L","Leche GLORIA evaporada 400g","Fideos DON VITTORIO spaghetti 500g","Harina BLANCA FLOR 1kg","Avena QUAKER 500g","Sal yodada EMSAL 1kg","Atun FLORIDA 170g","Cafe molido NESCAFÉ 250g","Te filtrante MCCOLINS 25 un","Galletas FIELD sabor vainilla","Chocolate SUBLIME 90g","Mermelada FANNY sabor fresa 310g","Mantequilla LAIVE 200g","Huevos LA CALERA 15un","Queso BONLÉ 500g","Yogur GLORIA 1L","Pan molde integral BIMBO","Cereal hojuelas ANGEL 1kg"},
                {"Papa blanca 1kg","Cebolla roja 1kg","Tomate 1kg","Zanahoria 1kg","Platano seda 1kg","Manzana roja 1kg","Naranja 1kg","Mandarina 1kg","Palta fuerte 1kg","Limon 1kg","Lechuga Americana Bolsa 300g","Brocoli 1kg","Espinaca Bolsa 150g","Pepino 1kg","Pimiento Morron 1kg","Uva Roja 1kg","Papaya 1kg","Pina Golden 1kg","Mango Edward 1kg","Ajo Entero Pelado 250g"},
                {"Pollo Entero 1kg","Pechuga Especial de Pollo 1kg","Pierna de Pollo 1kg","Carne Molida 1kg","Bistec de res 1kg","Lomo Fino Porcionado 1kg","Chuleta de Cerdo 1kg","Costilla de Cerdo 1kg","Trucha 1kg","Filete de Pescado 1kg","Bonito 1kg","Jurel 1kg","Salmon 500g","Langostinos 300g","Hamburguesa Bolsa 14un","Salchicha 500g","Jamon ingles 250g","Pavo 1kg","Higado de res 1kg","Molleja de Pollo 1kg"},
                {"Agua sin Gas 3L","Agua mineral 600 ml Pack 6un","Gaseosa COCA COLA sin Azucar","Gaseosa FANTA naranja 2.25L","Jugo naranja ECOFRESH 1L","Nectar FRUGOS DEL VALLE sabor durazno","Bebida Rehidratante GATORADE Tropical Fruit","Bebida Energizante RED BULL Lata 250ml","Leche Chocolatada GLORIA Caja 946ml","Bebida de Avena ORASI Barista Caja 1L","Bebida de Aloe Vera KERO Frutos Rojos 300ml","Limonada L'ONDA Pink 500ml","Agua Mineral SAN MATEO con Gas 1.5L","Bebida FRUGOS Sabor a Manzana 1.45L","Refresco TAMPICO Citrus Punch 3L","Bebida de Almendras NATURE'S HEART 946ml","Bebida de Naranja GLORIA 1.5L","Bebida de Chicha Morada GLORIA 1L","Jugo de Piña LA FLORENCIA 485ml","Agua saborizada LOA Mandarina 625ml"},
                {"Detergente en Polvo MARSELLA 4kg","Detergente Liquido ARIEL 3L x 2un","Lavavajilla Líquido SAPOLIO 1.25L","Lejia SAPOLIO Botella 4.5kg","Limpiador POETT Frescura de Lavanda 4.5L","Limpiavidrios Ecoamigable FROSCH 500ml","Limpiador POETT Frescura de Lavanda 900ml","Suavizante DOWNY Floral 2.6L","Esponja SCOTCH-BRITE Paquete 3un","Escoba VIRUTEX Clásica","Recogedor VIRUTEX","Trapeador POLINPLAST 13L","Paños Microfibra Multiuso 4un","Bolsas para Basura 35L x 30un","Guantes Limpieza VIRUTEX Talla M","Jabón de Lavar BOLIVAR barra 380g","Quitamanchas Líquido VANISH 1.8L","Ambientador en Aerosol GLADE 400ml","Papel Toalla ELITE 4rollos","Limpiador Líquido Multiusos DKASA 4L"},
                {"Shampoo Ácido Hialurónico ELVIVE 680ml","Acondicionador ELVIVE 680ml","Jabon de Tocador DOVE 90g Pack 3un","Pasta Dental Blanqueadora COLGATE 100ml","Cepillo Dental COLGATE Pack 4un","Enjuague Bucal LISTERINE 1.5L","Papel Higiénico SUAVE 40un","Desodorante en Aerosol REXONA 150ml x 2un","Crema Corporal NIVEA 1000ml","Protector Solar NEUTROGENA FPS70 - 40g","Alcohol Gel Antibacterial 400ml","Algodon COPPON Bolsa 100g","Hisopos COTONETES JOHNSON'S 150un","Toallas Higiénicas KOTEX Pack 60un","Pañales BABYSEC XXG 100un","Crema para Manos NIVEA Q10 75ml","Maquina de Afeitar GILLETE 2un","Espuma de Afeitar LEA 250ml","Peine Figaro Cortador","Jabón Líquido PROTEX 221ml"},
                {"Laptop LENOVO IPS 15'' Intel Core 5 / 8GB","Mouse LOGITECH M196 BR Negro","Teclado Mecánico Keychron Q6 Ultra","Audifonos Bluetooth JBL","Memoria USB KINGSTON 64 GB","IPAD 11'' APPLE 128GB","Smartphone MOTOROLA 128 GB","Cargador Apple iPhone 20W USB C","Parlante Bluetooth JBL Flip 7 Negro","Monitor TCL MiniLed 27'' 4K UHD","Webcam LOGITECH C270 HD 720P","Disco Sólido SSD Kingston 480GB SATA","Router Archer AX53 TP-Link WiFi 6","Impresora EPSON Multifunción L4360","Power Bank Xiaomi 33W 20000mAh","Cable HDMI Ugreen 2.0 4K - 2m","Micrófono GENIUS MIC S400U USB","Mouse Pad FIDDLER Gel FD-MP01 Negro","Smartwatch XIAOMI Redmi 5","HUB Multipuerto 4 USB 2.0 puerto"},
                {"Refrigeradora LG 282L","Licuadora OSTER 1.5L","Hervidor Eléctrico OSTER 1.7L","Plancha a Vapor OSTER","Microondas LG NeoChef 20L","Lavadora Mabe de 10kg Silver LMAP1015","Cocina a Gas BLACKLINE4 hornillas","Freidora de Aire OSTER 6L","Olla Arrocera OSTER 1.8L","Batidora de Mano OSTER","Sandwichera OSTER Platos Hondos","Ventilador 3 en 1 IMACO 50W","Aspiradora Vertical 2 en 1 VCL Stick","Cafetera OSTER PrimaLatte Touch","Tostadora BLACKLINE HBT-1613A","Secadora de Cabello SIEGEN SG-3035","Horno OSTER 60L","Congeladora TCL P200CFW 200L","Extractor OSTER FPSTJE316R","Procesador y tRITURADOR DE aLIMENTOS 2l 800W"},
                {"Juego de Ollas TRAMONTINA 8pzs","Sartén antiadherente TRAMONTINA 3pzs","Plato DECO HOME Tendido 28cm","Juego x 6pzs Vaso Alto Labrado 350ml","Juego de Cubiertos TRAMONTINA 24pzs","Almohada DRIMER Memory","Toalla de Baño DECO HOME 70x140cm","Sábana DECO HOME Kitty 1.5 Plazas","Edredón 2 Plazas Coffe DECO HOME","Organizador Armario Closet 3 CUerpos","Cesto Khipu REYPLAST 60L","Lámpara de Escritorio Flexible","Silla Ergonómica VIVA HOME","Mesa Catering Plegable 180cm","Cortina para Sala o Dormitorio 2 Paños","Taper DECO HOME","Tabla de Picar VIVA HOME 25cm","Cuchillo de Cocina FACUSA","Escurridor de Platos 2 Niveles Cloe","Termo 1L Acero Inoxidable"},
                {"Polo Básico de Algodón","Pantalón Jean Clásico CAYETANA","Casaca Impermeable","Zapatillas Urbanas Skechers","Gorra Deportiva Podium","Camisa Manga Larga Demin","Vestido Corto Mini Cleste","Blusa Manga Corta SINGULAR","Short Deportivo Básico","Conjunto Buzo Deportivo Podium","Chompa Cuello Redondo SINGULAR","Medias Pack 3 Pares Unisex","Zapatos Forman M-33 Canela Pasador","Sandalias Plana Ana Dama SINGULAR","Mochila Urbana k0003 Impermeable","Cinturón de Cuero Negro con Hebilla","Pijama Polo + Pantalón EUREKA","Leggings New Control Zafin","Sudadera Rosada Moda Genika","Bolso de Mano Art Gallery J-145"}
        };
        // Precios de cada categoría, en el mismo orden del arreglo CATEGORIAS.
        double[][] precios={
                {24.5,4.8,9.9,33.9,3.6,7,6.7,11.4,5.2,22.9,4.1,3.8,4.7,6.7,6.8,8.7,23.2,6.9,14.8,14.5},
                {4.99,4.39,6.5,4.39,3.89,8.99,9.69,4.89,10.97,3.99,3.29,5.9,4.9,3.49,13.49,15.5,5.49,10.58,6.59,12.99},
                {8.9,14.9,13.5,22.5,42.9,89.9,18.9,29.9,19.9,18,8.9,8.5,51.5,26.5,13.9,9.8,16.4,16.8,14.5,9.6},
                {3.2,10.5,9.9,6.2,14.49,4.6,2,6.9,6.5,14.9,3,3.5,2.9,5.5,7.3,11.5,5,4.2,5.6,1.7},
                {38,117.8,14.9,11.4,17.8,25.8,4.2,29.9,13.6,17.9,8.9,29.9,10.9,7.9,11.5,5.7,28.2,11,14.9,12.8},
                {37.9,37.9,13.5,16.9,18.9,37.4,26.9,25.9,64,71.2,45.9,7.5,14.5,14.5,90,27,16.9,17.5,25.9,8.5},
                {2989,54.9,1459,154.9,99,1719,649,109,599,2999,159,450,409,1399,207,70,199,19.9,159.9,26.9},
                {1449,249,159,69,399,1699,1399,419,159,129,99,199.9,159,1049,89,139,699,1099,289,109},
                {449,99.9,3.9,9.9,299,119,27.9,14.9,34.9,120,69.9,38,199,179,99,3.9,9.9,13.9,58.9,139.9},
                {90,179,149,199,39.9,44.9,239,39.9,39.9,83.3,40.9,4.9,199,24.9,159,90,29.9,69.9,139,269},
        };
        int id=1; //permitira identificar cada producto
        //Recorrer las categorias
        for (int componenteAux = 0; componenteAux < nombres.length; componenteAux++) {
            //Recorre los productos de cada categoria
            for (int columnaIndice = 0; columnaIndice < nombres[componenteAux].length; columnaIndice++) {
                double precio = precios[componenteAux][columnaIndice];
                catalogo.add(new Producto(id++,nombres[componenteAux][columnaIndice],CATEGORIAS[componenteAux],precio));
            }
        }
    }

    // ------------------------------------------------------------------------------
    // MÉTODO: buscarProducto
    // FUNCIÓN: Busca un producto por su identificador; devuelve null si no existe.
    // ------------------------------------------------------------------------------
    private Producto buscarProducto(int id) { //devolvera un objeto de clase producto

        //recorre directamente todos los objetos almacenados en el Arraylist catalogo
        for(Producto dato:catalogo) {
            //compara el identificador actual con el que estamos buscando
            if(dato.id==id) {
                return dato;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------
    // MÉTODO: formatoMoneda
    // FUNCIÓN: Convierte un importe a texto en soles con dos decimales.
    // -------------------------------------------------------------------
    private String formatoMoneda(double valorAux) { //dato en double → llevado a String
        return String.format(Locale.US,"S/ %.2f",valorAux);
    }

    // ------------------------------------------------------------------------------------------
    // MÉTODO: mostrarCatalogo
    // FUNCIÓN: Presenta productos filtrados por categoría y búsqueda, con opción de agregarlos.
    // ------------------------------------------------------------------------------------------
    private void mostrarCatalogo(String categoria,String consulta) {
        // Actualiza las variables globales del sistema con los parámetros recibidos
        categoriaSeleccionada=categoria; //parametro de categoria que se muestra inicialente
        busqueda=consulta; //contiene el texto utiliado para buscar productos
        JPanel crearPagina=crearPagina("Ofertas y productos para ti"); // crea el panel principal del catálogo con su respec titulo
        JPanel filtros=crearPanel(); //panel de selector de categoría y el campo busqueda
        //organizador de componenetes horizontalmente, comenzando desde la izquierda
        filtros.setLayout(new FlowLayout(FlowLayout.LEFT,10,10)); //crea un diseño con alineación
        JComboBox<String> selectorCategoria=new JComboBox<>(); //perm selecc una opcion mediante una lista desplegable
        selectorCategoria.addItem("Todas"); //agrega la primera opcion

        for(String componenteAux:CATEGORIAS) //recorremos el arreglo CATEGORIA (l37)
            selectorCategoria.addItem(componenteAux);

        selectorCategoria.setSelectedItem(categoria); //categoria que aparece inicialmente
        JTextField campoBusqueda=crearCampo(consulta); //crea un campo de texto vacio sin ancho definido

        // Agregamos los 4 componenetes al panel filtros
        filtros.add(crearEtiqueta("Categoría:",13,true));
        filtros.add(selectorCategoria);
        filtros.add(crearEtiqueta("Buscar:",13,true));
        filtros.add(campoBusqueda);

        //creamo panel donde mostrará los productos mediante tarjetas
        JPanel tarjetas=new JPanel(new GridLayout(0,4,12,12));
        tarjetas.setBackground(FONDO); //establ color de fondo

        //componenete de dezplazamiento por la cnatidad de productos
        JScrollPane desplazamiento=new JScrollPane(tarjetas);
        desplazamiento.getVerticalScrollBar().setUnitIncrement(18); //barra de desplaz vertical- establ 18 und el incremento de desplazamiento
        desplazamiento.setBorder(null); //eliminamos el borde exterior

        // Agrupa las instrucciones que reconstruyen el catálogo al cambiar los filtros.
        Runnable actualizar=()-> { //runnable - interfaz que permite almacenar un conjunto de instrucciones para ejecutar porteriomente
            categoriaSeleccionada=(String)selectorCategoria.getSelectedItem(); //obtiene la categoria selec por el usuario - devuelve un objeto de tip object (String) PARA CONVERTIR AL TIPO DE DATO REQUERIDO
            busqueda=campoBusqueda.getText().trim(); //Obtiene texto ingresado
            String normalizado=normalizar(busqueda); //llama el metood para convertir la busqueda en minuscula u eliminar las tildes
            tarjetas.removeAll(); //elimina todas las tarjetas anteriore para construir el catalogo de resultados actualiz
            int contador=0;

            //recorremos los objetos de la clase producto almacenado en el arraylist catalogo
            for(Producto elemento:catalogo) {

                // comprobamos si el producto pertenece a la categoría seleccionada
                if(!"Todas".equals(categoriaSeleccionada) && !categoriaSeleccionada.equals(elemento.categoria))
                    continue;
                // Omite los productos cuyo nombre no coincide con la búsqueda.
                if(!normalizar(elemento.nombre).contains(normalizado))
                    continue;

                contador++; //incrementa cuando supera ambos filtros

                //CREACIÓN DE LA TARJETA Y SU IMAGEN
                JPanel tarjeta=crearPanel(); //Creamos un panel individual
                tarjeta.setLayout(new BorderLayout(5,8)); //organiza la tarjeta por regiones
                //crea un borde compuesto por dos elementos
                tarjeta.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(new Color(229,230,234)),
                        new EmptyBorder(12,12,12,12))); //espacio c/l

                //creamos la etiqueta sin texto con alineacion horizontal centrada
                JLabel imagen=new JLabel("",SwingConstants.CENTER);
                imagen.setIcon(imagenProducto(elemento)); //llamamos al metodo imagenProducto(), enviandole el porducto actual
                imagen.setPreferredSize(new Dimension(180,115)); //tamaño de la etiqueta de imagen
                tarjeta.add(imagen,BorderLayout.NORTH); //coloca la imagen en la aprte superior de la tarjeta

                //Descripcion y precio del producto
                JPanel descripcion=crearPanel();
                descripcion.setLayout(new BoxLayout(descripcion,BoxLayout.Y_AXIS)); //organiza sus componenetes verticalmente
                descripcion.add(crearEtiqueta(elemento.categoria,11,false)); //categoria del producto

                //admitimos HTML básico dentro del componente JLabel - para limitar el ancho del texto a 165 px
                //si es largo se distribuye en varias lineas
                descripcion.add(crearEtiqueta(
                        "<html><div style='width:165px'>"+
                                elemento.nombre+"</div></html>",14,true));

                descripcion.add(Box.createVerticalStrut(6)); //agregamos espcio vertical
                //convertimos el precio numerico a texto mediante el metodo formatoMoneda
                JLabel precio=crearEtiqueta(formatoMoneda(elemento.precio),18,true);
                precio.setForeground(ROJO); //color texto
                descripcion.add(precio);
                descripcion.add(crearEtiqueta("Entrega disponible",11,false));
                tarjeta.add(descripcion,BorderLayout.CENTER);

                //agregar producto al carrito
                tarjeta.add(crearBoton("AGREGAR",()-> { //expresion lambda
                    // Agrega una unidad o suma una unidad a la cantidad existente.
                    carrito.merge(elemento.id,1,Integer::sum); //suma cantidad anterior con la nueva (LinkedHashMap<Integer, Integer>)
                    mostrarCatalogo(categoriaSeleccionada,busqueda); //sigue mostrando el catalogo
                }),BorderLayout.SOUTH); //finaliza lambda y coloca el boton en la parte inferior de la tarjeta
                tarjetas.add(tarjeta);
            } //fin ciclo for

            //actualización de la interfaz (comporbar si ningun producto coincidio con los filtros
            if(contador==0)
                tarjetas.add(crearEtiqueta("No se encontraron productos.",14,false));

            tarjetas.revalidate(); //vuelve a clacular la distribución y tamaño de os componentes
            tarjetas.repaint(); //dibuja los compnenetes graficos
            desplazamiento.getVerticalScrollBar().setValue(0); //regresa la barra dezplazamiento
        }; //fin runnable

        //buscqueda automática mediante eventos
        //cada que el usuarios seleccione otra categoria, se ejecuta el Runnable actualizar
        selectorCategoria.addActionListener(evento->actualizar.run());
        // Detecta cambios en el texto y actualiza los resultados automáticamente.
        campoBusqueda.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent evento) {
                actualizar.run();
            } //cuando agrega texte
            public void removeUpdate(javax.swing.event.DocumentEvent evento) { actualizar.run(); } //cuando elimina texto
            public void changedUpdate(javax.swing.event.DocumentEvent evento) {
                actualizar.run();
            }
        });

        //boton limpiar
        filtros.add(crearBoton("LIMPIAR",()-> {
            campoBusqueda.setText(""); //elimina el texto en el campo busqueda
            selectorCategoria.setSelectedItem("Todas"); //reestablece la cateria a todas
        }));

        //CONTRUCCION FINAL DE LA PANTALLA
        JPanel contenido=new JPanel(new BorderLayout(5,12));
        contenido.setBackground(FONDO);
        contenido.add(filtros,BorderLayout.NORTH); //Colocar el panel de filtros en la parte superior
        contenido.add(desplazamiento,BorderLayout.CENTER); //coloca catalogo con su barra de dezplazamiento
        crearPagina.add(contenido,BorderLayout.CENTER); //agrega el panel contenido dentr de la pagina principal
        mostrarPantalla(crearPagina,true); //muestra la pagina del catalogo y activa el encabezado de navegacion
        actualizar.run(); //ejecuta las intrucciones Runnable
    } //fin mostrar catalogo

    // --------------------------------------------------------------------------------
    // MÉTODO: normalizar
    // FUNCIÓN: Elimina tildes y convierte a minúsculas para facilitar las búsquedas.
    // --------------------------------------------------------------------------------
    private String normalizar(String valor) {
        return java.text.Normalizer.normalize(valor,java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).trim();
    }

    // ------------------------------------------------------------------------
    // MÉTODO: imagenProducto
    // FUNCIÓN: Carga la imagen del producto o genera una imagen de respaldo.
    // ------------------------------------------------------------------------
    private ImageIcon imagenProducto(Producto dato) { //ImageIcon - clase de javaswing permite cargar y representar imagenes como iconos dentro de compnentes gráficos

        //RECORRIDO DE LAS EXTENCIONES
        for(String extension:new String[]{"png","jpg","jpeg"}) { //crea un arreglo de tres elementos sin necesidad de almacenarlo en una variable independiente
            Path ruta=Paths.get("imagenes",dato.id+"."+extension); //construye la ubicacion del archivo que se desea buscar
            //VERIFICAR SI EXISTE EL ARCHIVO
            if(Files.exists(ruta)) { //devuelve un valor de tipo boolean
                ImageIcon icono=new ImageIcon(ruta.toString()); //cargar la imagen . recibe una ruta de tipo String

                //VALIDAR Y REDIMENSIONAR LA IMAGEN
                if(icono.getIconWidth()>0) //Obtenemos el ancho de la imagen en pix
                    //obtiene la imagen dentro de ImageIcon, y rediemnsiona la imagen
                    return new ImageIcon(icono.getImage().getScaledInstance(165,108,Image.SCALE_SMOOTH));
            }
        }

        //CREACION DE LA IMAGEN DE RESPALDO
        //permite crear y manipular imagenes directamente en la memoria de la computadora
        BufferedImage imagenBuffer=new BufferedImage(175,110,BufferedImage.TYPE_INT_RGB);
        Graphics2D grafico=imagenBuffer.createGraphics(); //crea un contexto grafico que permite dibujar sobre la imagen almacenada en imageBuffer

        //DIBUJAR EL FONDO - Establece un color y dibuja unrectangulo con el color seleccionado
        grafico.setColor(new Color(247,247,249)); //gris claro
        grafico.fillRect(0,0,175,110);

        //DIBUJA UN RECTANGULO REDONDEADO - Establece un color y dibujar un rectangulo con esquina redondeadas
        grafico.setColor(new Color(231,232,237));
        grafico.fillRoundRect(48,12,80,80,18,18);

        grafico.setColor(ROJO);
        //Configura la fuente, negrita y tamaño del texto.
        grafico.setFont(new Font("SansSerif",Font.BOLD,28));
        //Obtiene la primera letra del producto y la dibuja en mayúscula
        grafico.drawString(dato.nombre.substring(0,1).toUpperCase(Locale.ROOT),78,64);
        grafico.dispose(); //Libera los recursos del contexto gráfico.
        return new ImageIcon(imagenBuffer); //Devuelve la imagen de respaldo como un ImageIcon.
    }

    // ------------------------------------------------------------
    // MÉTODO: subtotal
    // FUNCIÓN: Suma el precio por cantidad de cada producto y redondea a dos decimales.
    // ------------------------------------------------------------
    private double subtotal() {
        double texto=0;
        for(Map.Entry<Integer,Integer> evento:carrito.entrySet()) {
            Producto dato=buscarProducto(evento.getKey());
            if(dato!=null)texto+=dato.precio*evento.getValue();
        }
        return Math.round(texto*100)/100.0;
    }


    // <<<<<<<<<<<<<<<<<<<< ALEX >>>>>>>>>>>>>>>>>>>>
    // Imagen del carrito: escala proporcional, sin recortar el producto.

    // ------------------------------------------------------------
    // MÉTODO: imagenProductoCarrito
    // FUNCIÓN: Ajusta la imagen proporcionalmente para evitar deformaciones.
    // ------------------------------------------------------------
    private ImageIcon imagenProductoCarrito(Producto dato) {
        for (String extension : new String[]{"png", "jpg", "jpeg"}) {
            Path ruta = Paths.get("imagenes", dato.id + "." + extension);
            if (!Files.exists(ruta)) continue;
            ImageIcon original = new ImageIcon(ruta.toString());
            int ancho = original.getIconWidth(), altoAux = original.getIconHeight();
            if (ancho <= 0 || altoAux <= 0) continue;
            double factor = Math.min(100.0 / ancho, 100.0 / altoAux);
            int nuevoAncho = Math.max(1, (int)Math.round(ancho * factor));
            int nuevoAlto = Math.max(1, (int)Math.round(altoAux * factor));
            return new ImageIcon(original.getImage().getScaledInstance(nuevoAncho, nuevoAlto, Image.SCALE_SMOOTH));
        }
        return imagenProducto(dato);
        // Imagen de respaldo si no existe el PNG.
    }

    // ------------------------------------------------------------
    // MÉTODO: mostrarCarrito
    // FUNCIÓN: Muestra los productos elegidos y permite cambiar cantidades o eliminarlos.
    // ------------------------------------------------------------
    private void mostrarCarrito() {
        JPanel crearPagina = crearPagina("Mi carrito de compras");
        JPanel centro = new JPanel(new BorderLayout());
        centro.setOpaque(false);
        JPanel elementos = new JPanel();
        elementos.setOpaque(false);
        elementos.setLayout(new BoxLayout(elementos, BoxLayout.Y_AXIS));
        elementos.setBorder(new EmptyBorder(12, 12, 12, 12));
        for (Map.Entry<Integer,Integer> entrada : new ArrayList<>(carrito.entrySet())) {
            Producto dato = buscarProducto(entrada.getKey());
            if (dato == null) continue;
            int cantidad = entrada.getValue();
            JPanel tarjeta = crearPanel();
            tarjeta.setLayout(new BorderLayout(14, 0));
            tarjeta.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(231,233,237), 1, true),
                    new EmptyBorder(15, 16, 15, 16)));
            tarjeta.setMaximumSize(new Dimension(690, 152));
            tarjeta.setPreferredSize(new Dimension(690, 152));
            tarjeta.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel image = new JLabel(imagenProductoCarrito(dato), SwingConstants.CENTER);
            image.setPreferredSize(new Dimension(112, 112));
            tarjeta.add(image, BorderLayout.WEST);
            JPanel informacion = crearPanel();
            informacion.setLayout(new BoxLayout(informacion, BoxLayout.Y_AXIS));
            JLabel marca = crearEtiqueta(dato.categoria.toUpperCase(Locale.ROOT), 10, false);
            marca.setForeground(new Color(117,124,136));
            informacion.add(marca);
            informacion.add(Box.createVerticalStrut(5));
            JLabel nombre = crearEtiqueta("<html><div style='width:255px'>" + dato.nombre + "</div></html>", 15, true);
            informacion.add(nombre);
            informacion.add(Box.createVerticalStrut(7));
            JLabel precio = crearEtiqueta(formatoMoneda(dato.precio), 14, true);
            precio.setForeground(ROJO);
            informacion.add(precio);
            informacion.add(Box.createVerticalGlue());
            JPanel controles = crearPanel();
            controles.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
            // Resta una unidad; elimina el producto si la cantidad llega a cero.
            JButton disminuir = crearBotonSecundario("−", () -> {
                int numeroAux = carrito.getOrDefault(dato.id, 0) - 1;
                if (numeroAux <= 0) carrito.remove(dato.id);
                else carrito.put(dato.id, numeroAux);
                mostrarCarrito();
            });
            // Suma una unidad al producto y vuelve a mostrar el carrito.
            JButton aumentar = crearBotonSecundario("+", () -> {
                carrito.merge(dato.id, 1, Integer::sum);
                mostrarCarrito();
            });
            JLabel etiquetaCantidad = crearEtiqueta(String.valueOf(cantidad), 14, true);
            etiquetaCantidad.setHorizontalAlignment(SwingConstants.CENTER);
            etiquetaCantidad.setPreferredSize(new Dimension(34, 32));
            controles.add(disminuir);
            controles.add(etiquetaCantidad);
            controles.add(aumentar);
            informacion.add(controles);
            tarjeta.add(informacion, BorderLayout.CENTER);
            JPanel derecha = crearPanel();
            derecha.setLayout(new BorderLayout());
            // Retira completamente el producto del carrito.
            JButton eliminar = crearBotonSecundario("✕", () -> {
                carrito.remove(dato.id);
                mostrarCarrito();
            });
            eliminar.setForeground(ROJO);
            JPanel contenedorEliminar = crearPanel();
            contenedorEliminar.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            contenedorEliminar.add(eliminar);
            derecha.add(contenedorEliminar, BorderLayout.NORTH);
            JLabel importeElemento = crearEtiqueta(formatoMoneda(dato.precio * cantidad), 13, true);
            importeElemento.setHorizontalAlignment(SwingConstants.RIGHT);
            derecha.add(importeElemento, BorderLayout.SOUTH);
            tarjeta.add(derecha, BorderLayout.EAST);
            elementos.add(tarjeta);
            elementos.add(Box.createVerticalStrut(12));
        }
        // Presenta una pantalla alternativa cuando no hay productos seleccionados.
        if (carrito.isEmpty()) {
            JPanel vacio = crearPanel();
            vacio.setBorder(new EmptyBorder(45, 35, 45, 35));
            vacio.setLayout(new BoxLayout(vacio, BoxLayout.Y_AXIS));
            JLabel mensaje = crearEtiqueta("Tu carrito está vacío", 18, true);
            mensaje.setAlignmentX(Component.CENTER_ALIGNMENT);
            vacio.add(mensaje);
            vacio.add(Box.createVerticalStrut(14));
            JButton explorar = crearBoton("VER PRODUCTOS", () -> mostrarCatalogo("Todas", ""));
            explorar.setAlignmentX(Component.CENTER_ALIGNMENT);
            vacio.add(explorar);
            elementos.add(vacio);
        }
        // Mantiene las tarjetas centradas aunque se maximice la ventana.
        JPanel contenedorLista = new JPanel(new BorderLayout());
        contenedorLista.setOpaque(false);
        JPanel columna = new JPanel(new BorderLayout());
        columna.setOpaque(false);
        columna.setPreferredSize(new Dimension(720, Math.max(150, carrito.size() * 164 + 25)));
        columna.add(elementos, BorderLayout.NORTH);
        contenedorLista.add(columna, BorderLayout.CENTER);
        JScrollPane desplazamiento = new JScrollPane(contenedorLista);
        desplazamiento.setBorder(null);
        desplazamiento.getViewport().setBackground(FONDO);
        desplazamiento.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JPanel listaCentrada = new JPanel(new BorderLayout());
        listaCentrada.setOpaque(false);
        listaCentrada.add(desplazamiento, BorderLayout.CENTER);
        centro.add(listaCentrada, BorderLayout.CENTER);
        JPanel pie = crearPanel();
        pie.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(229,231,235)),
                new EmptyBorder(16, 22, 16, 22)));
        pie.setLayout(new BorderLayout(12, 12));
        JPanel totales = crearPanel();
        totales.setLayout(new GridLayout(2, 2, 8, 8));
        totales.add(crearEtiqueta("Subtotal de productos:", 13, false));
        JLabel importeParcial = crearEtiqueta(formatoMoneda(subtotal()), 14, true);
        importeParcial.setHorizontalAlignment(SwingConstants.RIGHT);
        totales.add(importeParcial);
        totales.add(crearEtiqueta("Total del carrito:", 15, true));
        JLabel total = crearEtiqueta(formatoMoneda(subtotal()), 17, true);
        total.setForeground(ROJO);
        total.setHorizontalAlignment(SwingConstants.RIGHT);
        totales.add(total);
        pie.add(totales, BorderLayout.CENTER);
        JButton botonContinuar = crearBoton("CONTINUAR COMPRA", this::mostrarPago);
        // Impide continuar al pago si el carrito está vacío.
        botonContinuar.setEnabled(!carrito.isEmpty());
        pie.add(botonContinuar, BorderLayout.SOUTH);
        JPanel pieExterior = new JPanel(new GridBagLayout());
        pieExterior.setOpaque(false);
        pie.setPreferredSize(new Dimension(690, 145));
        pieExterior.add(pie);
        crearPagina.add(centro, BorderLayout.CENTER);
        crearPagina.add(pieExterior, BorderLayout.SOUTH);
        mostrarPantalla(crearPagina, true);
    }

    // ------------------------------------------------------------
    // MÉTODO: mostrarPago
    // FUNCIÓN: Presenta métodos de pago, puntos, saldo y monto pendiente.
    // ------------------------------------------------------------
    private void mostrarPago() {
        if(carrito.isEmpty()) {
            mensaje("El carrito está vacío.");
            return;
        }
        JPanel crearPagina=crearPagina("Confirma compra");
        JPanel cuerpo=crearPanel();
        cuerpo.setLayout(new BoxLayout(cuerpo,BoxLayout.Y_AXIS));
        cuerpo.setBorder(new EmptyBorder(24,24,24,24));
        cuerpo.setPreferredSize(new Dimension(520,640));
        JTextField direccion=crearCampo(usuarioActual.direccion);
        direccion.setPreferredSize(new Dimension(300,34));
        direccion.setMaximumSize(new Dimension(300,34));
        JComboBox<String> metodo=new JComboBox<>(new String[]{"Tarjeta de crédito","Tarjeta de débito","Yape","Plin"});
        metodo.setPreferredSize(new Dimension(300,34));
        JPanel adicional=crearPanel();
        adicional.setLayout(new BoxLayout(adicional,BoxLayout.Y_AXIS));
        adicional.setAlignmentX(Component.CENTER_ALIGNMENT);
        JCheckBox usarPuntos=new JCheckBox("Canjear puntos disponibles");
        usarPuntos.setBackground(Color.WHITE);
        JLabel etiquetaTotal=crearEtiqueta("",15,true);
        Runnable refrescar=()-> {
            double descuento=usarPuntos.isSelected()&&subtotal()>50?Math.min(usuarioActual.puntos*0.50,subtotal()):0;
            double pendiente=Math.max(0,subtotal()-descuento-usuarioActual.saldo);
            etiquetaTotal.setText("<html><div style='text-align:center'>Compra: "+formatoMoneda(subtotal())+
                    " &nbsp; | &nbsp; Descuento: "+formatoMoneda(descuento)+
                    "<br>Falta pagar: "+formatoMoneda(pendiente)+"</div></html>");
        };
        cuerpo.add(centrarPago(crearEtiqueta("Dirección de entrega",14,true)));
        cuerpo.add(centrarPago(direccion));
        cuerpo.add(Box.createVerticalStrut(16));
        cuerpo.add(centrarPago(crearEtiqueta("Saldo disponible en PLAZA VEA: "+formatoMoneda(usuarioActual.saldo),15,true)));
        cuerpo.add(Box.createVerticalStrut(5));
        cuerpo.add(centrarPago(crearEtiqueta("Puntos: "+usuarioActual.puntos+" (valor "+formatoMoneda(usuarioActual.puntos*0.50)+")",13,false)));
        cuerpo.add(centrarPago(usarPuntos));
        usarPuntos.addActionListener(evento->refrescar.run());
        cuerpo.add(Box.createVerticalStrut(15));
        cuerpo.add(centrarPago(etiquetaTotal));
        cuerpo.add(Box.createVerticalStrut(22));
        cuerpo.add(centrarPago(crearEtiqueta("Método para pagar la diferencia",14,true)));
        cuerpo.add(centrarPago(metodo));
        cuerpo.add(Box.createVerticalStrut(10));
        cuerpo.add(adicional);
        Runnable renderizar=()->mostrarCamposPago(adicional,(String)metodo.getSelectedItem());
        metodo.addActionListener(evento->renderizar.run());
        renderizar.run();
        refrescar.run();
        JPanel acciones=crearPanel();
        acciones.setLayout(new FlowLayout(FlowLayout.CENTER,10,0));
        acciones.add(crearBotonSecundario("Volver",this::mostrarCarrito));
        acciones.add(crearBoton("CONFIRMAR Y GENERAR BOLETA",()-> {
            if(!validarCamposPago(adicional,(String)metodo.getSelectedItem()))return;
            confirmarCompra((String)metodo.getSelectedItem(),direccion.getText().trim(),usarPuntos.isSelected());
        }));
        cuerpo.add(Box.createVerticalStrut(22));
        cuerpo.add(centrarPago(acciones));
        JPanel soporte=new JPanel(new GridBagLayout());
        soporte.setOpaque(false);
        GridBagConstraints restricciones=new GridBagConstraints();
        restricciones.anchor=GridBagConstraints.NORTH;
        restricciones.insets=new Insets(14,0,18,0);
        soporte.add(cuerpo,restricciones);
        JScrollPane desplazamiento=new JScrollPane(soporte);
        desplazamiento.setBorder(null);
        desplazamiento.getViewport().setBackground(FONDO);
        desplazamiento.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        crearPagina.add(desplazamiento,BorderLayout.CENTER);
        mostrarPantalla(crearPagina,true);
    }

    // ------------------------------------------------------------
    // MÉTODO: centrarPago
    // FUNCIÓN: Centra un componente dentro de una fila del formulario de pago.
    // ------------------------------------------------------------
    private JPanel centrarPago(JComponent componente) {
        JPanel linea=crearPanel();
        linea.setLayout(new FlowLayout(FlowLayout.CENTER,0,5));
        linea.setAlignmentX(Component.CENTER_ALIGNMENT);
        linea.setMaximumSize(new Dimension(Integer.MAX_VALUE,Math.max(42,componente.getPreferredSize().height+10)));
        linea.add(componente);
        return linea;
    }

    // ------------------------------------------------------------
    // MÉTODO: filaCompacta
    // FUNCIÓN: Alinea un componente a la izquierda con altura controlada.
    // ------------------------------------------------------------
    private JPanel filaCompacta(JComponent componente) {
        JPanel fila=crearPanel();
        fila.setLayout(new FlowLayout(FlowLayout.LEFT,0,4));
        fila.setAlignmentX(Component.LEFT_ALIGNMENT);
        int alto = Math.max(48, componente.getPreferredSize().height + 12);
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, alto));
        fila.setPreferredSize(new Dimension(componente.getPreferredSize().width + 4, alto));
        fila.add(componente);
        return fila;
    }

    // ------------------------------------------------------------
    // MÉTODO: mostrarCamposPago
    // FUNCIÓN: Cambia los campos visibles según el medio de pago seleccionado.
    // ------------------------------------------------------------
    private void mostrarCamposPago(JPanel adicional,String metodo) {
        adicional.removeAll();
        adicional.setAlignmentX(Component.CENTER_ALIGNMENT);
        if(metodo.startsWith("Tarjeta")) {
            adicional.add(centrarPago(crearEtiqueta("Número de tarjeta de prueba (16 dígitos)",12,true)));
            JTextField numero=new JTextField();
            numero.setName("cardNumber");
            numero.setPreferredSize(new Dimension(300,34));
            adicional.add(centrarPago(numero));
            adicional.add(centrarPago(crearEtiqueta("No introduzcas datos bancarios reales.",11,false)));
        } else {
            adicional.add(centrarPago(crearEtiqueta("QR ilustrativo de "+metodo+" (no permite pagos reales)",12,true)));
            JLabel qr=new JLabel(generarQRDemostracion(metodo));
            qr.setHorizontalAlignment(SwingConstants.CENTER);
            qr.setVerticalAlignment(SwingConstants.CENTER);
            qr.setPreferredSize(new Dimension(185,185));
            JPanel panelQR=crearPanel();
            panelQR.setLayout(new FlowLayout(FlowLayout.CENTER,0,8));
            panelQR.setAlignmentX(Component.CENTER_ALIGNMENT);
            panelQR.setMaximumSize(new Dimension(Integer.MAX_VALUE,205));
            panelQR.setPreferredSize(new Dimension(320,205));
            panelQR.add(qr);
            adicional.add(panelQR);
        }
        adicional.revalidate();
        adicional.repaint();
    }

    // ------------------------------------------------------------
    // MÉTODO: generarQRDemostracion
    // FUNCIÓN: Dibuja un patrón QR ilustrativo que no procesa pagos reales.
    // ------------------------------------------------------------
    private ImageIcon generarQRDemostracion(String metodo) {
        int numeroAux=29,celda=5;
        BufferedImage imagenBuffer=new BufferedImage(numeroAux*celda,numeroAux*celda,BufferedImage.TYPE_INT_RGB);
        Graphics2D grafico=imagenBuffer.createGraphics();
        grafico.setColor(Color.WHITE);
        grafico.fillRect(0,0,imagenBuffer.getWidth(),imagenBuffer.getHeight());
        Random filaAux=new Random(metodo.hashCode());
        for(int coordenadaY=0;coordenadaY<numeroAux;coordenadaY++)for(int coordenadaX=0;coordenadaX<numeroAux;coordenadaX++) {
            boolean marcador=(coordenadaX<8&&coordenadaY<8)||(coordenadaX>=numeroAux-8&&coordenadaY<8)||(coordenadaX<8&&coordenadaY>=numeroAux-8);
            if(marcador)continue;
            if(filaAux.nextBoolean()) {
                grafico.setColor(Color.BLACK);
                grafico.fillRect(coordenadaX*celda,coordenadaY*celda,celda,celda);
            }
        }
        dibujarMarcador(grafico,0,0,celda);
        dibujarMarcador(grafico,numeroAux-7,0,celda);
        dibujarMarcador(grafico,0,numeroAux-7,celda);
        grafico.dispose();
        return new ImageIcon(imagenBuffer);
    }

    // ------------------------------------------------------------
    // MÉTODO: dibujarMarcador
    // FUNCIÓN: Dibuja los cuadrados de referencia del patrón QR.
    // ------------------------------------------------------------
    private void dibujarMarcador(Graphics2D grafico,int coordenadaX,int coordenadaY,int componenteAux) {
        grafico.setColor(Color.BLACK);
        grafico.fillRect(coordenadaX*componenteAux,coordenadaY*componenteAux,7*componenteAux,7*componenteAux);
        grafico.setColor(Color.WHITE);
        grafico.fillRect((coordenadaX+1)*componenteAux,(coordenadaY+1)*componenteAux,5*componenteAux,5*componenteAux);
        grafico.setColor(Color.BLACK);
        grafico.fillRect((coordenadaX+2)*componenteAux,(coordenadaY+2)*componenteAux,3*componenteAux,3*componenteAux);
    }

    // ------------------------------------------------------------
    // MÉTODO: validarCamposPago
    // FUNCIÓN: Comprueba que el número ficticio de tarjeta tenga 16 dígitos.
    // ------------------------------------------------------------
    private boolean validarCamposPago(JPanel adicional,String metodo) {
        if(!metodo.startsWith("Tarjeta"))return true;
        JTextField crearCampo=buscarCampoTarjeta(adicional);
        if(crearCampo==null||!crearCampo.getText().replace(" ","").matches("[0-9]{16}")) {
            mensaje("Ingresa 16 dígitos ficticios de prueba, no una tarjeta real.");
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------
    // MÉTODO: buscarCampoTarjeta
    // FUNCIÓN: Busca recursivamente el campo de tarjeta dentro de los contenedores.
    // ------------------------------------------------------------
    private JTextField buscarCampoTarjeta(Container contenedorBusqueda) {
        for(Component componenteAux:contenedorBusqueda.getComponents()) {
            if(componenteAux instanceof JTextField && "cardNumber".equals(componenteAux.getName()))return (JTextField)componenteAux;
            if(componenteAux instanceof Container) {
                JTextField resultado=buscarCampoTarjeta((Container)componenteAux);
                if(resultado!=null)return resultado;
            }
        }
        return null;
    }

    // ------------------------------------------------------------
    // MÉTODO: confirmarCompra
    // FUNCIÓN: Valida la compra, calcula descuentos y saldo, registra el pedido y genera la boleta.
    // ------------------------------------------------------------
    private void confirmarCompra(String metodo,String direccion,boolean usar) {
        double base=subtotal();
        if(base<=0)return;
        if(modalidadEntrega.equals("DOMICILIO")&&direccion.isEmpty()) {
            mensaje("Ingrese una dirección.");
            return;
        }
        if(usar&&base<=50) {
            mensaje("El canje requiere una compra superior a S/ 50.");
            return;
        }
        // Calcula cuántos puntos se pueden utilizar sin superar el importe de compra.
        int canjeados=usar?Math.min(usuarioActual.puntos,(int)Math.floor(base/0.50)):0;
        // Cada punto vale S/ 0.50; resta el descuento y redondea el total.
        double descuento=canjeados*0.50,total=Math.round((base-descuento)*100)/100.0;
        // Utiliza como máximo el saldo necesario y calcula el pago restante.
        double saldoUsado=Math.min(usuarioActual.saldo,total),pagoExterno=Math.round((total-saldoUsado)*100)/100.0;
        // Otorga 10 puntos si el total después del descuento supera S/ 100.
        int ganados=total>100?10:0;
        String resumen="Subtotal: "+formatoMoneda(base)+"\nDescuento: "+formatoMoneda(descuento)+"\nSaldo usado: "+formatoMoneda(saldoUsado)+"\nPago por "+metodo+": "+formatoMoneda(pagoExterno)+"\nPuntos ganados: "+ganados+"\n\n¿Confirmar compra de prueba?";
        if(JOptionPane.showConfirmDialog(this,resumen,"Confirmar",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        StringBuilder elementos=new StringBuilder();
        for(Map.Entry<Integer,Integer> evento:carrito.entrySet()) {
            Producto dato=buscarProducto(evento.getKey());
            elementos.append(dato.nombre).append(" x").append(evento.getValue()).append("; ");
        }
        // Genera el identificador del pedido con fecha, hora y contador.
        String id="PM"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))+"-"+(contadorPedidos++);
        Pedido pedido=new Pedido(id,usuarioActual.correo,LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),"En curso",elementos.toString(),metodo,direccion,total);
        // Guarda los valores anteriores para recuperarlos si ocurre un error de escritura.
        int puntosAnteriores=usuarioActual.puntos;
        double saldoAnterior=usuarioActual.saldo;
        String direccionAnterior=usuarioActual.direccion;
        // Descuenta los puntos usados y añade los obtenidos.
        usuarioActual.puntos=usuarioActual.puntos-canjeados+ganados;
        usuarioActual.saldo=Math.round((usuarioActual.saldo-saldoUsado)*100)/100.0;
        usuarioActual.direccion=direccion;
        pedidos.add(pedido);
        try {
            Files.createDirectories(carpetaBoletas);
            guardarUsuarios();
            guardarPedidos();
            // Genera el TXT antes de vaciar el carrito, pues necesita sus productos.
            Path boleta=generarBoleta(pedido,base,descuento,canjeados,ganados);
            // Elimina los productos seleccionados del carrito.
            carrito.clear();
            mensaje("Compra registrada. Boleta TXT:\n"+boleta.toAbsolutePath());
            mostrarPedidos("Todos");
        } catch(IOException excepcion) {
            usuarioActual.puntos=puntosAnteriores;
            usuarioActual.saldo=saldoAnterior;
            usuarioActual.direccion=direccionAnterior;
            pedidos.remove(pedido);
            mostrarError(excepcion);
        }
    }

    // ------------------------------------------------------------
    // MÉTODO: generarBoleta
    // FUNCIÓN: Construye el comprobante de texto y lo escribe en un archivo TXT.
    // ------------------------------------------------------------
    private Path generarBoleta(Pedido pedido,double base,double descuento,int usados,int ganados)throws IOException {
        // Construye la ruta del comprobante utilizando el ID del pedido.
        Path salida=carpetaBoletas.resolve("BOLETA_"+pedido.id+".txt");
        // Acumula las líneas de texto que formarán el comprobante.
        StringBuilder boton=new StringBuilder();
        boton.append("==========================================\n").append("                PLAZA VEA\n").append("          COMPROBANTE DE PRUEBA\n").append("      NO VALIDO PARA EFECTOS TRIBUTARIOS\n").append("==========================================\n").append("Pedido: ").append(pedido.id).append("\nFecha: ").append(pedido.fecha).append("\nCliente: ").append(usuarioActual.nombre).append("\n").append("Entrega: ").append(modalidadEntrega).append("\n").append("------------------------------------------\n");
        for(Map.Entry<Integer,Integer> evento:carrito.entrySet()) {
            Producto dato=buscarProducto(evento.getKey());
            boton.append(dato.nombre).append("\n").append(String.format(Locale.US,"  %d x %8.2f    S/ %8.2f\n",evento.getValue(),dato.precio,evento.getValue()*dato.precio));
        }
        boton.append("------------------------------------------\n").append(String.format(Locale.US,"SUBTOTAL:               S/ %10.2f\n",base)).append(String.format(Locale.US,"DESCUENTO POR PUNTOS:    S/ %10.2f\n",descuento)).append(String.format(Locale.US,"TOTAL A PAGAR:          S/ %10.2f\n",pedido.total)).append("Pago: ").append(pedido.pago).append("\nPuntos canjeados: ").append(usados).append("\nPuntos ganados: ").append(ganados).append("\nPuntos disponibles: ").append(usuarioActual.puntos).append("\n==========================================\n").append("   Gracias por comprar en PLAZA VEA\n").append("     DOCUMENTO ACADEMICO - NO SUNAT\n");
        // Escribe el comprobante en UTF-8 para conservar caracteres especiales.
        Files.write(salida,boton.toString().getBytes(StandardCharsets.UTF_8));
        return salida;
    }

    // <<<<<<<<<<<<<<<<<<<< JONATAN >>>>>>>>>>>>>>>>>>>>
    // ------------------------------------------------------------
    // MÉTODO: mostrarPedidos
    // FUNCIÓN: Presenta los pedidos del usuario, sus filtros, detalles, boletas y cancelación.
    // ------------------------------------------------------------
    private void mostrarPedidos(String filtro) {
        filtroPedidosActivo=filtro;
        JPanel crearPagina=crearPagina("Mis pedidos");
        JPanel contenido=new JPanel(new BorderLayout(10,12));
        contenido.setBackground(FONDO);
        JPanel pestanas=crearPanel();
        pestanas.setLayout(new FlowLayout(FlowLayout.LEFT));

        for(String texto:new String[]{"Todos","En curso","Entregados","Cancelados"})pestanas.add(crearBotonSecundario(texto,()->mostrarPedidos(texto)));
        contenido.add(pestanas,BorderLayout.NORTH);
        // Define las columnas y filas de la tabla de pedidos.
        DefaultTableModel modelo=new DefaultTableModel(new String[]{"Pedido","Fecha","Estado","Total","Pago"},0) {
            // Impide editar directamente las celdas de la tabla.
            public boolean isCellEditable(int filaAux,int componenteAux) {
                return false;
            }
        };
        for(Pedido pedido:pedidos)if(pedido.correo.equalsIgnoreCase(usuarioActual.correo)&&(filtro.equals("Todos")||pedido.estado.equals(filtro)))
            modelo.addRow(new Object[]{pedido.id,pedido.fecha,pedido.estado,formatoMoneda(pedido.total),pedido.pago});
        JTable tabla=new JTable(modelo);
        tabla.setRowHeight(35);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        contenido.add(new JScrollPane(tabla),BorderLayout.CENTER);
        JPanel piePedidos=crearPanel();
        piePedidos.setLayout(new FlowLayout(FlowLayout.RIGHT));
        piePedidos.add(crearBotonSecundario("VER DETALLE",()-> {
            Pedido pedido=pedidoSeleccionado(tabla);
            if(pedido==null)return;
            mensaje("Pedido: "+pedido.id+"\nEstado: "+pedido.estado+"\nProductos: "+pedido.elementos+"\nTotal: "+formatoMoneda(pedido.total));
        }));
        piePedidos.add(crearBotonSecundario("ABRIR BOLETA",()-> {
            Pedido pedido=pedidoSeleccionado(tabla);
            if(pedido==null)return;
            Path boleta=carpetaBoletas.resolve("BOLETA_"+pedido.id+".txt");
            if(!Files.exists(boleta)) {
                mensaje("No se encontró la boleta de este pedido.");
                return;
            }
            try {
                if(Desktop.isDesktopSupported()&&Desktop.getDesktop().isSupported(Desktop.Action.OPEN))
                    Desktop.getDesktop().open(boleta.toFile());
                else mensaje("Boleta guardada en: "+boleta.toAbsolutePath());
            } catch(IOException excepcion) {
                mostrarError(excepcion);
            }
        }));
        piePedidos.add(crearBoton("CANCELAR PEDIDO",()-> {
            Pedido pedido=pedidoSeleccionado(tabla);
            if(pedido==null)return;
            // Solo permite cancelar pedidos que todavía están en curso.
            if(!"En curso".equals(pedido.estado)) {
                mensaje("Solo puedes cancelar pedidos en curso.");
                return;
            }
            if(JOptionPane.showConfirmDialog(this,"¿Cancelar "+pedido.id+"?","Confirmar",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
            // Cambia el estado del pedido seleccionado.
            pedido.estado="Cancelados";
            try {
                guardarPedidos();
                mostrarPedidos(filtro);
            } catch(IOException excepcion) {
                pedido.estado="En curso";
                mostrarError(excepcion);
            }
        }));
        contenido.add(piePedidos,BorderLayout.SOUTH);
        crearPagina.add(contenido,BorderLayout.CENTER);
        crearPagina.putClientProperty("ordersView",true);
        mostrarPantalla(crearPagina,true);
    }

    // ------------------------------------------------------------
    // MÉTODO: pedidoSeleccionado
    // FUNCIÓN: Recupera el objeto Pedido correspondiente a la fila seleccionada.
    // ------------------------------------------------------------
    private Pedido pedidoSeleccionado(JTable tabla) {
        int filaAux=tabla.getSelectedRow();
        if(filaAux<0) {
            mensaje("Selecciona un pedido.");
            return null;
        }
        String id=String.valueOf(tabla.getValueAt(filaAux,0));
        for(Pedido pedido:pedidos)if(pedido.id.equals(id))return pedido;
        return null;
    }

    // ------------------------------------------------------------
    // MÉTODO: actualizarEntregas
    // FUNCIÓN: Cambia a entregado los pedidos en curso con al menos 60 segundos de antigüedad.
    // ------------------------------------------------------------
    private void actualizarEntregas() {
        boolean cambio=false;
        long ahora=System.currentTimeMillis();
        for(Pedido pedido:pedidos)if("En curso".equals(pedido.estado)&&ahora-pedido.momentoCreacion>=60000) {
            pedido.estado="Entregados";
            cambio=true;
        }
        if(cambio) {
            try {
                guardarPedidos();
            } catch(IOException excepcion) {
                System.err.println(excepcion.getMessage());
            }
            if(filtroPedidosActivo!=null&&usuarioActual!=null)mostrarPedidos(filtroPedidosActivo);
        }
    }

    // ------------------------------------------------------------
    // MÉTODO: mostrarPerfil
    // FUNCIÓN: Presenta datos personales, puntos, saldo y acciones de la cuenta.
    // ------------------------------------------------------------
    private void mostrarPerfil() {
        JPanel crearPagina=crearPagina("Mi cuenta / Perfil");
        JPanel contenido=crearPanel();
        contenido.setLayout(new BorderLayout(22,20));
        contenido.setBorder(new EmptyBorder(28,30,28,30));
        JPanel superior=crearPanel();
        superior.setLayout(new BorderLayout(28,0));
        JPanel avatar=crearPanel();
        avatar.setLayout(new BorderLayout());
        avatar.setPreferredSize(new Dimension(190,205));
        avatar.setBorder(BorderFactory.createLineBorder(new Color(222,224,230)));
        JLabel retrato=new JLabel("\u263A",SwingConstants.CENTER);
        retrato.setFont(new Font("SansSerif",Font.PLAIN,88));
        retrato.setForeground(ROJO);
        avatar.add(retrato,BorderLayout.CENTER);
        JLabel leyenda=crearEtiqueta("PERFIL",15,true);
        leyenda.setHorizontalAlignment(SwingConstants.CENTER);
        avatar.add(leyenda,BorderLayout.SOUTH);
        superior.add(avatar,BorderLayout.WEST);
        JPanel detalles=crearPanel();
        detalles.setLayout(new BoxLayout(detalles,BoxLayout.Y_AXIS));
        detalles.add(crearEtiqueta("Datos personales",22,true));
        detalles.add(Box.createVerticalStrut(15));
        for(String linea:new String[]{"Nombre: "+usuarioActual.nombre,"Correo: "+usuarioActual.correo,
                "Teléfono: "+usuarioActual.telefono,"Dirección: "+usuarioActual.direccion}) {
            detalles.add(crearEtiqueta(linea,15,false));
            detalles.add(Box.createVerticalStrut(12));
        }
        detalles.add(crearEtiqueta("Puntos: "+usuarioActual.puntos+"  |  Valor: "+formatoMoneda(usuarioActual.puntos*0.50),17,true));
        detalles.add(Box.createVerticalStrut(12));
        detalles.add(crearEtiqueta("Saldo disponible: "+formatoMoneda(usuarioActual.saldo),19,true));
        superior.add(detalles,BorderLayout.CENTER);
        contenido.add(superior,BorderLayout.NORTH);
        JPanel acciones=crearPanel();
        acciones.setLayout(new FlowLayout(FlowLayout.LEFT,10,5));
        acciones.add(crearBoton("RECARGAR SALDO",this::mostrarRecarga));
        acciones.add(crearBoton("EDITAR DATOS",this::editarPerfil));
        acciones.add(crearBoton("CAMBIAR CONTRASEÑA",this::cambiarContrasena));
        contenido.add(acciones,BorderLayout.CENTER);
        JPanel inferior=crearPanel();
        inferior.setLayout(new FlowLayout(FlowLayout.LEFT,0,0));
        inferior.add(crearBoton("CERRAR SESIÓN",()-> {
            usuarioActual=null;
            // Elimina los productos seleccionados del carrito.
            carrito.clear();
            mostrarInicioSesion();
        }));
        contenido.add(inferior,BorderLayout.SOUTH);
        crearPagina.add(contenido,BorderLayout.CENTER);
        mostrarPantalla(crearPagina,true);
    }

    // ------------------------------------------------------------
    // MÉTODO: cambiarContrasena
    // FUNCIÓN: Verifica la clave actual y guarda la nueva si cumple las condiciones.
    // ------------------------------------------------------------
    private void cambiarContrasena() {
        JPasswordField claveAnterior=new JPasswordField(20);
        JPasswordField claveNueva=new JPasswordField(20);
        JPasswordField repetirClave=new JPasswordField(20);
        Object[] entradas={"Contraseña actual:",claveAnterior,"Nueva contraseña:",claveNueva,
                "Repetir nueva contraseña:",repetirClave};
        if(JOptionPane.showConfirmDialog(this,entradas,"Cambiar contraseña",
                JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        String anterior=new String(claveAnterior.getPassword());
        String nueva=new String(claveNueva.getPassword());
        String repetida=new String(repetirClave.getPassword());
        if(!usuarioActual.contrasena.equals(anterior)) {
            mensaje("La contraseña actual no es correcta.");
            return;
        }
        if(nueva.length()<6) {
            mensaje("La nueva contraseña debe tener al menos 6 caracteres.");
            return;
        }
        if(!nueva.equals(repetida)) {
            mensaje("Las nuevas contraseñas no coinciden.");
            return;
        }
        String previa=usuarioActual.contrasena;
        usuarioActual.contrasena=nueva;
        try {
            guardarUsuarios();
            mensaje("Contraseña actualizada.");
            mostrarPerfil();
        }
        catch(IOException excepcion) {
            usuarioActual.contrasena=previa;
            mostrarError(excepcion);
        }
    }

    // ------------------------------------------------------------
    // MÉTODO: mostrarRecarga
    // FUNCIÓN: Valida el importe y registra una recarga de demostración.
    // ------------------------------------------------------------
    private void mostrarRecarga() {
        JPanel crearPagina=crearPagina("Recargar saldo PLAZA VEA");
        JPanel formulario=crearPanel();
        formulario.setLayout(new BoxLayout(formulario,BoxLayout.Y_AXIS));
        formulario.setBorder(new EmptyBorder(28,35,28,35));
        JTextField monto=new JTextField("50.00");
        monto.setPreferredSize(new Dimension(245,32));
        JComboBox<String> metodo=new JComboBox<>(new String[]{"Tarjeta de crédito","Tarjeta de débito","Yape","Plin"});
        metodo.setPreferredSize(new Dimension(265,32));
        JPanel adicional=crearPanel();
        adicional.setLayout(new BoxLayout(adicional,BoxLayout.Y_AXIS));
        formulario.add(crearEtiqueta("Saldo actual: "+formatoMoneda(usuarioActual.saldo),17,true));
        formulario.add(Box.createVerticalStrut(16));
        formulario.add(crearEtiqueta("Monto a recargar (S/)",13,true));
        formulario.add(filaCompacta(monto));
        formulario.add(Box.createVerticalStrut(12));
        formulario.add(crearEtiqueta("Método de recarga",13,true));
        formulario.add(filaCompacta(metodo));
        formulario.add(Box.createVerticalStrut(10));
        formulario.add(adicional);
        Runnable renderizar=()->mostrarCamposPago(adicional,(String)metodo.getSelectedItem());
        metodo.addActionListener(evento->renderizar.run());
        renderizar.run();
        JPanel acciones=crearPanel();
        acciones.setLayout(new FlowLayout(FlowLayout.LEFT,10,0));
        acciones.add(crearBotonSecundario("VOLVER AL PERFIL",this::mostrarPerfil));
        acciones.add(crearBoton("CONFIRMAR RECARGA",()-> {
            double valor;
            try {
                valor=Double.parseDouble(monto.getText().trim().replace(",","."));
            }
            catch(NumberFormatException excepcion) {
                mensaje("Monto inválido.");
                return;
            }
            if(!Double.isFinite(valor)||valor<=0||valor>10000) {
                mensaje("Ingresa un monto entre S/ 0.01 y S/ 10,000.");
                return;
            }
            if(!validarCamposPago(adicional,(String)metodo.getSelectedItem()))return;
            if(JOptionPane.showConfirmDialog(this,"Recarga de prueba por "+formatoMoneda(valor)+". ¿Confirmar?","Recarga",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
            double anterior=usuarioActual.saldo;
            usuarioActual.saldo=Math.round((anterior+valor)*100)/100.0;
            try {
                guardarUsuarios();
                mensaje("Recarga de demostración realizada.");
                mostrarPerfil();
            }
            catch(IOException excepcion) {
                usuarioActual.saldo=anterior;
                mostrarError(excepcion);
            }
        }));
        formulario.add(Box.createVerticalStrut(20));
        formulario.add(filaCompacta(acciones));
        JPanel superior=crearPanel();
        superior.setLayout(new BorderLayout());
        superior.add(formulario,BorderLayout.NORTH);
        crearPagina.add(new JScrollPane(superior),BorderLayout.CENTER);
        mostrarPantalla(crearPagina,true);
    }

    // ------------------------------------------------------------
    // MÉTODO: editarPerfil
    // FUNCIÓN: Permite modificar nombre, teléfono y dirección y guardar los cambios.
    // ------------------------------------------------------------
    private void editarPerfil() {
        JTextField nombre=crearCampo(usuarioActual.nombre),telefono=crearCampo(usuarioActual.telefono),direccion=crearCampo(usuarioActual.direccion);
        Object[] campos={"Nombre",nombre,"Telefono",telefono,"Direccion",direccion};
        if(JOptionPane.showConfirmDialog(this,campos,"Editar perfil",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        String numeroAux=nombre.getText().trim(),datoAux=telefono.getText().trim(),auxiliar=direccion.getText().trim();
        // |!evento.matches|Valida nombre, estructura del correo y longitud mínima de la clave.
        if(numeroAux.isEmpty()||(numeroAux+datoAux+auxiliar).contains("|")) {
            mensaje("Datos invalidos.");
            return;
        }
        String nombreAnterior=usuarioActual.nombre,telefonoAnterior=usuarioActual.telefono,direccionAnterior=usuarioActual.direccion;
        usuarioActual.nombre=numeroAux;
        usuarioActual.telefono=datoAux;
        usuarioActual.direccion=auxiliar;
        try {
            guardarUsuarios();
            mostrarPerfil();
        } catch(IOException excepcion) {
            usuarioActual.nombre=nombreAnterior;
            usuarioActual.telefono=telefonoAnterior;
            usuarioActual.direccion=direccionAnterior;
            mostrarError(excepcion);
        }
    }

    // ------------------------------------------------------------
    // MÉTODO: codificar
    // FUNCIÓN: Convierte texto a Base64 para almacenarlo; no es cifrado.
    // ------------------------------------------------------------
    private String codificar(String texto) {
        return Base64.getEncoder().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------
    // MÉTODO: decodificar
    // FUNCIÓN: Recupera el texto original a partir de Base64.
    // ------------------------------------------------------------
    private String decodificar(String texto) {
        return new String(Base64.getDecoder().decode(texto),StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------
    // MÉTODO: cargarUsuarios
    // FUNCIÓN: Lee el TXT y reconstruye los objetos Usuario y sus saldos.
    // ------------------------------------------------------------
    private void cargarUsuarios() {
        try {
            Files.createDirectories(datos);
            if(!Files.exists(archivoUsuarios))return;
            for(String linea:Files.readAllLines(archivoUsuarios,StandardCharsets.UTF_8)) {
                try {
                    // Separa los campos de cada registro usando el carácter vertical como delimitador.
                    String[] auxiliar=linea.split("\\|",-1);
                    if(auxiliar.length>=6)usuarios.add(new Usuario(decodificar(auxiliar[0]),decodificar(auxiliar[1]),decodificar(auxiliar[2]),decodificar(auxiliar[3]),decodificar(auxiliar[4]),Integer.parseInt(auxiliar[5])));
                    if(auxiliar.length>=7)usuarios.get(usuarios.size()-1).saldo=Double.parseDouble(auxiliar[6]);
                } catch(Exception excepcion) {
                    System.err.println("Usuario mal formado omitido");
                }
            }
        } catch(IOException excepcion) {
            mostrarError(excepcion);
        }
    }

    // ------------------------------------------------------------
    // MÉTODO: guardarUsuarios
    // FUNCIÓN: Escribe los datos de todos los usuarios en el TXT.
    // ------------------------------------------------------------
    private void guardarUsuarios()throws IOException {
        List<String> lineas=new ArrayList<>();
        for(Usuario usuario:usuarios)lineas.add(codificar(usuario.nombre)+"|"+codificar(usuario.correo)+"|"+codificar(usuario.contrasena)+"|"+codificar(usuario.direccion)+"|"+codificar(usuario.telefono)+"|"+usuario.puntos+"|"+usuario.saldo);
        Files.createDirectories(datos);
        // Guarda la lista completa de usuarios en el archivo de texto.
        Files.write(archivoUsuarios,lineas,StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------
    // MÉTODO: cargarPedidos
    // FUNCIÓN: Lee el TXT y reconstruye el historial y tiempos de creación.
    // ------------------------------------------------------------
    private void cargarPedidos() {
        try {
            if(!Files.exists(archivoPedidos))return;
            for(String linea:Files.readAllLines(archivoPedidos,StandardCharsets.UTF_8)) {
                try {
                    // Separa los campos de cada registro usando el carácter vertical como delimitador.
                    String[] auxiliar=linea.split("\\|",-1);
                    if(auxiliar.length>=8)pedidos.add(new Pedido(decodificar(auxiliar[0]),decodificar(auxiliar[1]),decodificar(auxiliar[2]),decodificar(auxiliar[3]),decodificar(auxiliar[4]),decodificar(auxiliar[5]),decodificar(auxiliar[6]),Double.parseDouble(auxiliar[7])));
                    if(auxiliar.length>=9)pedidos.get(pedidos.size()-1).momentoCreacion=Long.parseLong(auxiliar[8]);
                } catch(Exception excepcion) {
                    System.err.println("Pedido mal formado omitido");
                }
            }
        } catch(IOException excepcion) {
            mostrarError(excepcion);
        }
    }

    // ------------------------------------------------------------
    // MÉTODO: guardarPedidos
    // FUNCIÓN: Escribe todos los pedidos y sus estados en el TXT.
    // ------------------------------------------------------------
    private void guardarPedidos()throws IOException {
        List<String> lineas=new ArrayList<>();
        for(Pedido pedido:pedidos)lineas.add(codificar(pedido.id)+"|"+codificar(pedido.correo)+"|"+codificar(pedido.fecha)+"|"+codificar(pedido.estado)+"|"+codificar(pedido.elementos)+"|"+codificar(pedido.pago)+"|"+codificar(pedido.direccion)+"|"+pedido.total+"|"+pedido.momentoCreacion);
        Files.createDirectories(datos);
        // Guarda la lista completa de pedidos en el archivo de texto.
        Files.write(archivoPedidos,lineas,StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------
    // MÉTODO: mensaje
    // FUNCIÓN: Muestra una ventana informativa al usuario.
    // ------------------------------------------------------------
    private void mensaje(String texto) {
        JOptionPane.showMessageDialog(this,texto,"PLAZA VEA",JOptionPane.INFORMATION_MESSAGE);
    }

    // ------------------------------------------------------------
    // MÉTODO: mostrarError
    // FUNCIÓN: Muestra el mensaje de una excepción en una ventana de error.
    // ------------------------------------------------------------
    private void mostrarError(Exception excepcion) {
        JOptionPane.showMessageDialog(this,"Error: "+excepcion.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
    }

    // ------------------------------------------------------------
    // MÉTODO: main
    // FUNCIÓN: Punto de entrada: inicia la interfaz en el hilo de eventos de Swing.
    // ------------------------------------------------------------
    public static void main(String[] argumentos) {
        // Ejecuta la creación de la ventana en el hilo de eventos de Swing.
        SwingUtilities.invokeLater(PlazaVea::new);
    }
}