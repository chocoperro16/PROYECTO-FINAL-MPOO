package tienda.ui;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import tienda.articulos.Articulo;
import tienda.servicio.Tienda;
import tienda.usuarios.Invitado;
import tienda.usuarios.Usuario;
import tienda.usuarios.UsuarioRegistrado;

/** Catalogo por categorias y acceso al carrito, historial y cuenta. */
class VentanaTienda extends JFrame {

    private final Tienda tienda;
    private final Usuario usuario;
    private final JTree arbol;

    private final JPanel contenido = new JPanel();

    private final JButton botonCarrito =
            Estilo.boton("Carrito (0)", true);

    private final JTextField buscar = new JTextField(16);

    private final JLabel estado = new JLabel(" ");

    private final Runnable oyente =
            () -> SwingUtilities.invokeLater(this::refrescar);

    private String filtroCategoria = null;
    private String filtroTipo = null;

    private boolean cerrando = false;
    private boolean cerrada = false;

    // COLORES NEON
    private static final Color FONDO_CATALOGO = new Color(0x292536);

    private static final Color FONDO_CATEGORIAS = new Color(0xD6D2DE);

    private static final Color TEXTO_CATEGORIAS = new Color(0x1D132B);

    private static final Color TURQUESA_NEON = new Color(0x20D4C2);

    private static final Color ROSA_NEON = new Color(0xF04483);

    private static final Color NARANJA_NEON = new Color(0xFF9F1C);

    private static final Color MORADO_NEON = new Color(0x782BCE);

    VentanaTienda(Tienda tienda, Usuario usuario) {

        super("Tienda MPOO - " + usuario.getNombre());

        this.tienda = tienda;
        this.usuario = usuario;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // ENCABEZADO
        JPanel norte = Estilo.encabezado("Tienda en Linea MPOO", (usuario instanceof Invitado ? "Invitado (tu carrito no se guarda)"
                        : "Hola, " + usuario.getNombre() + "  <" + usuario.getCorreo() + ">"));

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));

        acciones.setOpaque(false);

        acciones.add(new JLabel("<html><font color='white'>Buscar:</font></html>"));

        // BUSCADOR (SIN CAMBIOS)
        acciones.add(buscar);

        // CARRITO
        acciones.add(botonCarrito);

        // BOTONES DE USUARIO REGISTRADO
        if (usuario instanceof UsuarioRegistrado) {

            JButton historial = botonColor("Mis pedidos", TURQUESA_NEON, new Color(0x172A35));

            JButton cuenta = botonColor("Eliminar cuenta", ROSA_NEON, Color.WHITE);

            historial.addActionListener(e ->
                    new DialogoHistorial(
                            this,
                            tienda,
                            (UsuarioRegistrado) usuario
                    ).setVisible(true)
            );

            cuenta.addActionListener(e -> eliminarCuenta());

            acciones.add(historial);
            acciones.add(cuenta);
        }

        // CERRAR SESION
        JButton salir = botonColor("Cerrar sesion", NARANJA_NEON, new Color(0x292536));

        salir.addActionListener(e -> dispose());

        acciones.add(salir);

        norte.add(acciones, BorderLayout.EAST);

        add(norte, BorderLayout.NORTH);

        // CATEGORIAS
        DefaultMutableTreeNode raiz = new DefaultMutableTreeNode("Todo el catalogo");

        for (Map.Entry<String, String[]> c
                : Tienda.SUBCATEGORIAS.entrySet()) {

            DefaultMutableTreeNode n =
                    new DefaultMutableTreeNode(c.getKey());

            for (String s : c.getValue()) {
                n.add(new DefaultMutableTreeNode(s));
            }

            raiz.add(n);
        }

        arbol = new JTree(new DefaultTreeModel(raiz));

        for (int i = 0; i < arbol.getRowCount(); i++) {
            arbol.expandRow(i);
        }

        arbol.setFont(Estilo.NORMAL);
        arbol.setBackground(FONDO_CATEGORIAS);
        arbol.setForeground(TEXTO_CATEGORIAS);
        arbol.setOpaque(true);
        arbol.setBorder(new EmptyBorder(8, 8, 8, 8));

        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setTextNonSelectionColor(TEXTO_CATEGORIAS);
        renderer.setTextSelectionColor(Color.WHITE);
        renderer.setBackgroundNonSelectionColor(new Color(0, 0, 0, 0));
        renderer.setBackgroundSelectionColor(MORADO_NEON);
        renderer.setBorderSelectionColor(MORADO_NEON);
        arbol.setCellRenderer(renderer);

        arbol.addTreeSelectionListener(e ->
                seleccionar(e.getPath())
        );

        JScrollPane izq = new JScrollPane(arbol);

        izq.setPreferredSize(new Dimension(195, 400));
        izq.setBackground(FONDO_CATEGORIAS);
        izq.getViewport().setBackground(FONDO_CATEGORIAS);
        izq.getViewport().setOpaque(true);
        izq.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, MORADO_NEON));

        add(izq, BorderLayout.WEST);

        // CATALOGO
        contenido.setLayout(
                new BoxLayout(contenido, BoxLayout.Y_AXIS)
        );

        contenido.setBackground(FONDO_CATALOGO);

        JScrollPane centro = new JScrollPane(contenido);

        centro.setBackground(FONDO_CATALOGO);
        centro.getViewport().setBackground(FONDO_CATALOGO);
        centro.getVerticalScrollBar().setUnitIncrement(18);

        // CAMBIO DE COLOR DEL RECTANGULO DEL CATALOGO
        centro.setBorder(BorderFactory.createEmptyBorder());
        centro.getViewport().setOpaque(true);

        add(centro, BorderLayout.CENTER);

        // BARRA INFERIOR
        estado.setBorder(
                new EmptyBorder(4, 10, 4, 10)
        );

        estado.setFont(Estilo.CHICA);
        estado.setForeground(Color.WHITE);
        estado.setOpaque(true);
        estado.setBackground(FONDO_CATALOGO);

        add(estado, BorderLayout.SOUTH);

        // EVENTOS
        botonCarrito.addActionListener(e ->
                new DialogoCarrito(
                        this,
                        tienda,
                        usuario
                ).setVisible(true)
        );

        buscar.addActionListener(e -> refrescar());

        tienda.agregarOyente(oyente);

        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosed(WindowEvent e) {

                cerrada = true;

                tienda.quitarOyente(oyente);

                tienda.cerrarSesion(usuario);
            }
        });

        setSize(1040, 700);

        refrescar();
    }

    // BOTONES PERSONALIZADOS
    private JButton botonColor(String texto, Color fondo, Color textoColor) {

        JButton b = new JButton(texto);

        b.setFont(Estilo.NORMAL.deriveFont(Font.BOLD, 13f));

        b.setForeground(textoColor);
        b.setBackground(fondo);

        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorderPainted(false);

        b.setBorder(
                BorderFactory.createEmptyBorder(
                        9, 16, 9, 16
                )
        );

        // Bordes redondeados de FlatLaf
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.putClientProperty("JButton.arc", 22);

        b.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        return b;
    }

    private void seleccionar(TreePath ruta) {

        filtroCategoria = null;
        filtroTipo = null;

        if (ruta != null && ruta.getPathCount() >= 2) {
            filtroCategoria =
                    ruta.getPathComponent(1).toString();
        }

        if (ruta != null && ruta.getPathCount() >= 3) {
            filtroTipo =
                    ruta.getPathComponent(2).toString();
        }

        refrescar();
    }

    void refrescar() {

        if (cerrando || cerrada) {
            return;
        }

        if (!tienda.sigueActivo(usuario)) {

            cerrando = true;

            Estilo.aviso(
                    this,
                    "Tu cuenta fue eliminada por el administrador. "
                    + "Se cerrara la sesion."
            );

            dispose();

            return;
        }

        contenido.removeAll();

        String texto =
                buscar.getText().trim().toLowerCase();

        int mostrados = 0;
        // lista.size
        String[] categorias =
                filtroCategoria == null
                        ? Tienda.CATEGORIAS
                        : new String[]{filtroCategoria};

        for (String cat : categorias) {

            Map<String, List<Articulo>> subs =
                    tienda.consultarCategoria(cat);

            contenido.add(
                    titulo(cat, 18, new Color(0xE6D7FF))
            );

            for (Map.Entry<String, List<Articulo>> s
                    : subs.entrySet()) {

                if (filtroTipo != null
                        && !filtroTipo.equals(s.getKey())) {
                    continue;
                }

                List<Articulo> lista = new ArrayList<>();

                for (Articulo a : s.getValue()) {

                    if (texto.isEmpty()
                            || a.getNombre().toLowerCase()
                            .contains(texto)) {

                        lista.add(a);
                    }
                }

                contenido.add(
                        titulo(
                                s.getKey() + "  (" + lista.size() + ")", 14, new Color(0x20D4C2)
                        )
                );

                JPanel rejilla = new JPanel(
                        new GridLayout(0, 4, 10, 10)
                );

                rejilla.setOpaque(false);

                rejilla.setBorder(
                        new EmptyBorder(0, 14, 10, 14)
                );

                rejilla.setAlignmentX(LEFT_ALIGNMENT);

                if (lista.isEmpty()) {

                    JLabel v = new JLabel(
                            "Sin articulos en esta subcategoria"
                    );

                    v.setFont(Estilo.CHICA);
                    v.setForeground(new Color(0xD6D2DE));

                    rejilla.add(v);
                }

                for (Articulo a : lista) {

                    rejilla.add(tarjeta(a));

                    mostrados++;
                }

                contenido.add(rejilla);
            }
        }

        contenido.add(Box.createVerticalGlue());

        int enCarrito =
                Tienda.carritoDe(usuario).getCantidad();

        botonCarrito.setText(
                "Carrito (" + enCarrito + ")"
        );

        estado.setText(
                mostrados
                + " articulo(s) mostrados  |  carrito: "
                + enCarrito
                + "/15  |  catalogo v"
                + tienda.getVersion()
        );

        contenido.revalidate();
        contenido.repaint();
    }

    private static JComponent titulo(
            String texto,
            int tam,
            Color color
    ) {

        JLabel l = new JLabel(texto);

        l.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        tam
                )
        );

        l.setForeground(color);

        l.setBorder(
                new EmptyBorder(10, 14, 4, 14)
        );

        l.setAlignmentX(LEFT_ALIGNMENT);

        return l;
    }

    // TARJETAS DE PRODUCTOS
    private JPanel tarjeta(Articulo a) {

        JPanel t = new JPanel(
                new BorderLayout(4, 4)
        );

        t.setBackground(
                new Color(0xD6D2DE)
        );

        t.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(0x782BCE),
                                2
                        ),
                        new EmptyBorder(8, 8, 8, 8)
                )
        );

        t.add(
                Estilo.imagen(
                        tienda.resolverImagen(a),
                        a.getNombre(),
                        180,
                        110
                ),
                BorderLayout.NORTH
        );

        boolean disponible = a.estaDisponible();

        JLabel info = new JLabel(
                "<html><b>"
                + Estilo.escapar(a.getNombre())
                + "</b><br>"
                + "<font size='5' color='#782BCE'><b>"
                + Estilo.dinero(a.getPrecioFinal())
                + "</b></font><br>"
                + "<font color='#5B6472'>"
                + Estilo.escapar(Estilo.leyendaPrecio(a))
                + "</font><br>"
                + (disponible
                        ? "<font color='#16845B'>Stock: "
                        + a.getStock()
                        + "</font>"
                        : "<font color='#C62828'><b>"
                        + "Agotado / no disponible"
                        + "</b></font>")
                + "</html>"
        );

        info.setFont(Estilo.NORMAL);

        t.add(info, BorderLayout.CENTER);

        JButton agregar = botonColor(
                "Agregar",
                MORADO_NEON,
                Color.WHITE
        );

        agregar.setEnabled(disponible);

        agregar.addActionListener(e ->
                Estilo.intentar(this, () -> {

                    tienda.agregarAlCarrito(
                            usuario,
                            a.getId()
                    );

                    estado.setText(
                            "Se agrego \""
                            + a.getNombre()
                            + "\" al carrito"
                    );

                    refrescar();
                })
        );

        t.add(agregar, BorderLayout.SOUTH);

        return t;
    }

    // ELIMINAR CUENTA
    private void eliminarCuenta() {

        JPasswordField pass =
                new JPasswordField();

        int r = JOptionPane.showConfirmDialog(
                this,
                new Object[]{
                    "Esta accion borra tu cuenta y tu carrito. " + "Tus pedidos se conservan en el historial "
                    + "de la tienda.", "Confirma con tu contrasena:", pass
                },
                "Eliminar cuenta",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (r != JOptionPane.OK_OPTION) {
            return;
        }

        String p = new String(pass.getPassword());

        cerrando = true;

        Estilo.enSegundoPlano(
                this,
                () -> {
                    tienda.eliminarCuenta(
                            (UsuarioRegistrado) usuario, p);
                    return true;
                },
                ok -> {
                    Estilo.info(this, "Cuenta eliminada", "Tu cuenta se elimino correctamente.");
                    dispose();
                }
        );

        Timer t = new Timer(
                1500,
                e -> {
                    if (!cerrada && tienda.sigueActivo(usuario)) {
                        cerrando = false;
                    }
                }
        );
        t.setRepeats(false);
        t.start();
    }
}