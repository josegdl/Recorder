package intelite.recorder;

import intelite.hilos.HiloCanal;
import intelite.models.Canal;
import intelite.models.Config;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import org.apache.commons.lang3.math.Fraction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *
 * @author Desarrollo
 */
public class RecorderApp {

    private static final Logger LOG = LoggerFactory.getLogger(RecorderApp.class);
    public static final String DS = System.getProperty("file.separator").equals("\\") ? "\\" : "/";
    public static final boolean ISLINUX = DS.equals("/");
    public static final String FFMPEG_PATH = System.getProperty("user.dir") + DS;
    private static final String PROPERTIES_PATH = System.getProperty("user.dir") + DS;

   

    private static final String CONTENT_CONFIG
            = "#### DESCOMENTAR LÍNEAS QUE INICIEN CON #> (QUITAR # y >) #####\n\n"
            + "# ********** CONFIGURACIÓN DE VIDEO **********\n"
            + "\n"
            + "# ----- Formato de salida -----\n"
            + "# Valores: mkv, mp3, mp4, ts, wmv, asf\n"
            + "# Descripción: establece el formato de salida de la grabación (valor por defecto: mkv)\n"
            + ">fto=mkv\n"
            + "# -------------------------\n"
            + "\n"
            + "# ----- Duración de la grabación (bloque) -----\n"
            + "# Valores: un valor entero (valores sugeridos: 1, 5, 10, 15, 30, 60, 120)\n"
            + "# Descripción: indica el tiempo (en minutos) que debe durar cada grabación o bloque (valor por defecto: 60 minutos)\n"
            + ">dur=60\n"
            + "# -------------------------\n"
            + "\n"
            + "# ----- Resolución -----\n"
            + "# Valores: 640x480, 720x576, 1280x720, 1920x1080\n"
            + "# Descripción: establece la resolución de salida del video (valor por defecto: 1280x720)\n"
            + ">res=1280x720\n"
            + "# -------------------------\n"
            + "\n"
            + "# ----- Bitrate video (kbps) -----\n"
            + "# Valores: un valor entero (valor sugerido entre 100 y 1000)\n"
            + "# Descripción: establece el bitrate del video (valor por defecto: 750)\n"
            + ">vbr=750\n"
            + "# -------------------------\n"
            + "\n"
            + "# ----- Fotogramas por segundo (fps) -----\n"
            + "# Valores: un valor entero (valores sugeridos: 24, 25, 30, 60)\n"
            + "# Descripción: indica el valor de la propiedad fps del video (valor por defecto: 30)\n"
            + ">fps=30\n"
            + "# -------------------------\n"
            + "\n"
            + "# ********** CONFIGURACIÓN DE AUDIO **********\n"
            + "\n"
            + "# ----- Samplerate audio (Hz) -----\n"
            + "# Valores: un valor entero (valores sugeridos: 32000, 44100, 48000)\n"
            + "# Descripción: indica la velocidad de muestra de sonido (valor por defecto: 32000)\n"
            + ">asr=32000\n"
            + "# -------------------------\n"
            + "\n"
            + "# ----- Bitrate audio (kbps) -----\n"
            + "# Valores: un valor entero (valores sugeridos: 48, 64, 96, 128, 160, 192, 256)\n"
            + "# Descripción: establece el bitrate del audio (valor por defecto: 96)\n"
            + ">abr=96\n"
            + "# -------------------------";

    private static final String CONTENT_CANALES
            = "# ********** CONFIGURACIÓN DE CANALES A GRABAR **********\n"
            + "\n"
            + "# ===== Campos que conforman el registro de cada canal: =====\n"
            + "# CANAL: establece el nombre del canal, debe contener solo letras, números y/o guiones bajos o medios (p. ej. 34-2). Este campo no puede estar vacio.\n"
            + "# ALIAS: establece el alias del canal, usado para crear la carpeta del canal, debe contener solo letras, números y/o guiones bajos o medios (p. ej. TV34-2b). Este campo no puede estar vacio.\n"
            + "# ORIGEN: indica el origen de la grabación, origen del streaming (p. ej. http://99.90.149.52/live.m3u8). Este campo no puede estar vacio.\n"
            + "# DESTINO: indica el directorio de destino de la grabación (p. ej. SO WINDOWS=C:\\\\Users\\\\Usuario\\\\GrabacionesTV, SO LINUX=/opt/GrabacionesTV). Este campo no puede estar vacio. \n"
            + "# ACTIVO: indica si el canal está activo para grabarse, los valores pueden 0 (inactivo) y 1 (activo). Si no se indica un valor, el valor por defecto será 1 (activo).  \n"
            + "\n"
            + "# ===== Formato para registrar un canal: =====\n"
            + "# CANAL=ALIAS, ORIGEN, DESTINO, ACTIVO\n"
            + "\n"
            + "# ===== Ejemplo de registro de un canal en SO Windows: =====\n"
            + "# 34-2=TV34-2b, http://99.90.149.52/live.m3u8, C:\\\\Users\\\\Usuario\\\\GrabacionesTV, 1\n"
            + "\n"
            + "# ===== Ejemplo de registro de un canal en SO Linux: =====\n"
            + "# 34-2=TV34-2b, http://99.90.149.52/live.m3u8, /opt/GrabacionesTV, 1\n"
            + "\n"
            + "\n"
            + "# /////////////// REGISTRO DE CANALES A GRABAR ///////////////";

    private static final String CONTENT_HORARIOS
            = "# ********** CONFIGURACIÓN DE HORARIOS DE GRABACIÓN **********\n"
            + "\n"
            + "# ===== Campos que conforman el registro de un horario: =====\n"
            + "# CANAL: indica el nombre del canal, debe coincidir con el registrado en el archivo \"canales.properties\" \n"
            + "# DIA: un valor entero entre 1 y 7 que indica el día de la semana (Domingo=1, Lunes=2, Martes=3, Miercoles=4, Jueves=5, Viernes=6, Sabado=7)\n"
            + "# HORA_INI: indica la hora de inicio para deshabilitar el proceso de grabación\n"
            + "# HORA_FIN: indica hasta que hora durará deshabilitado el proceso de grabación\n"
            + "\n"
            + "# ===== Formato para registrar un horario: =====\n"
            + "# CANAL=DIA>HORA_INI-HORA_FIN\n"
            + "\n"
            + "# ===== Ejemplo de registro de un canal en SO Windows: =====\n"
            + "# 34-2=1>2-5, 2>2-5, 3>2-5, 4>2-5, 5>2-5, 6>2-5, 7>2-5 (indica que de lunes a viernes no se va a grabar de las 02:00 a las 05:00 hrs.)\n"
            + "\n"
            + "\n"
            + "# [Domingo=1, Lunes=2, Martes=3, Miercoles=4, Jueves=5, Viernes=6, Sabado=7, DIARIO=8]\n"
            + "# ===== HORARIO GENERAL (APLICA PARA TODOS LOS CANALES)  =====\n"
            + "#all=8>2-5\n"
            + "\n"
            + "# /////////////// HORARIO ESPECIFICO (APLICA SOLO PARA EL CANAL INDICADO) ///////////////\n"
            + "#34-2=6>10-11\n"
            + "#c1=1>2-5, 2>2-5, 3>2-5, 4>2-5, 5>2-5, 6>2-5, 7>2-5\n"
            + "#cN=1>2-5, 2>2-5, 3>2-5, 4>2-5, 5>2-5, 6>2-5, 7>2-5\n"
            + "";

    public static void main(String args[]) {
//        LOG.error("===== RECORDER v.20210915[10+] =====\n");
//        LOG.error("===== RECORDER v.20211227 =====\n"); // 10 min m´sa después de la hora
//       LOG.error("===== RECORDER v.20220110 =====\n");
        LOG.error("===== RECORDER v.20231128 =====\n");
        
        TelegramNotifier telegramNotifier = new TelegramNotifier("6896378553:AAG-QKi1zA9HIYe0rkO2Alsfkzww6egXb5o","-1001869238197");
        
        String botToken = "6896378553:AAG-QKi1zA9HIYe0rkO2Alsfkzww6egXb5o";
        String chatId = "-1001869238197";

        // Llama al método sendMessage
        sendMessage(botToken, chatId, "Recorder");
        
        Properties conf = new Properties();
        Properties canales = new Properties();
        Properties horarios = new Properties();

        String path_conf = PROPERTIES_PATH + "config.properties";
        File conf_file = new File(path_conf);
        if (!conf_file.exists()) {
            LOG.debug("Creando archivo config.properties...");
            Properties p = new Properties();
            try {
                p.store(new FileWriter("config.properties"), CONTENT_CONFIG);
            } catch (IOException e) {
                LOG.error("No se pudo crear el archivo 'config.properties' en la ruta " + path_conf, e);
            }
        }

        String path_canales = PROPERTIES_PATH + "canales.properties";
        File canales_file = new File(path_canales);
        if (!canales_file.exists()) {
            LOG.debug("Creando archivo canales.properties...");
            Properties p = new Properties();
            try {
                p.store(new FileWriter("canales.properties"), CONTENT_CANALES);
            } catch (IOException e) {
                LOG.error("No se pudo crear el archivo 'canales.properties' en la ruta " + path_canales, e);
            }
        }

        String path_horarios = PROPERTIES_PATH + "horarios.properties";
        File horarios_file = new File(path_horarios);
        if (!horarios_file.exists()) {
            LOG.debug("Creando archivo horarios.properties...");
            Properties p = new Properties();
            try {
                p.store(new FileWriter("horarios.properties"), CONTENT_HORARIOS);
            } catch (IOException e) {
                LOG.error("No se pudo crear el archivo 'horarios.properties' en la ruta " + path_horarios, e);
            }
        }

        try {
            conf.load(new FileReader(path_conf));
            canales.load(new FileReader(path_canales));
            horarios.load(new FileReader(path_horarios));
            iniciarGrabacion(conf, canales, horarios, telegramNotifier);
            
            
        } catch (FileNotFoundException e) {
            LOG.error("No se pudo encontrar o cargar alguno de los archivos de configuración!", e);
            close(telegramNotifier);
        } catch (IOException e) {
            LOG.error("No se pudo abrir alguno de los archivos de configuración!", e);
            close(telegramNotifier);
        }
    }

    // Variables declaration - do not modify                     
    // End of variables declaration                   
    private static void iniciarGrabacion(Properties conf, Properties canales, Properties horarios, TelegramNotifier telegramNotifier) {
        String fto = conf.getProperty("fto", "mkv").toLowerCase();

        String dur_s = conf.getProperty("dur", "60");
        Long dur = Long.parseLong(dur_s) * 60;

        String res_s = conf.getProperty("res", "1280x720");
        String[] txt = res_s.split("x");
        Integer res_w = Integer.parseInt(txt[0]);
        Integer res_h = Integer.parseInt(txt[1]);

        String vbr_s = conf.getProperty("vbr", "750");
        Long vbr = Long.parseLong(vbr_s) * 1000;

        String fps_s = conf.getProperty("fps", "30");
        Fraction fps = Fraction.getFraction(Integer.parseInt(fps_s), 1);

        String asr_s = conf.getProperty("asr", "32000");
        Integer asr = Integer.parseInt(asr_s);

        String abr_s = conf.getProperty("abr", "96");
        Long abr = Long.parseLong(abr_s) * 1000;

        // Se crea el objeto de tipo Config
        Config config = new Config(fto, dur, res_w, res_h, vbr, fps, asr, abr);

        System.out.println("*** CONFIGURACIÓN ***");
        System.out.println("> Formato de salida video: " + fto);
        System.out.println("> Duración bloque grabación: " + dur_s + " minutos");
        if (fto.equals("mkv")) {
            System.out.println("> Audio: configuración de origen");
            System.out.println("> Video: configuración de origen");
        } else {
            System.out.println("> Resolución: " + res_s);
            System.out.println("> Bitrate video: " + vbr_s + " kbps");
            System.out.println("> Fotogramas por segundo: " + fps_s + " fps");
            System.out.println("> Samplerate audio: " + asr_s + " Hz");
            System.out.println("> Bitrate audio: " + abr_s + " kbps");
        }
        System.out.println("");

        LOG.debug("Inicia obtención de canales a grabar...");
        telegramNotifier.sendMessage("Inicia optencion de canales a grabar...");
        Enumeration<Object> keys = canales.keys();
        if (keys.hasMoreElements()) {
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
//                System.out.println(key + " = " + canales.get(key));
                String[] d = canales.get(key).toString().trim().replace(" ", "").split(",");
                String nombre = key.toString();
                String alias = d[0];
                String origen = d[1];
                String destino = setBackslash(d[2]);
                Integer activo = d.length > 3 ? Integer.parseInt(d[3]) : 1;
                // Se obtienen los horarios
                String[] horario_g = {};
                String[] horario_e = {};
                String all = horarios.getProperty("all", "");
                // Se obtiene el horario general
                if (!all.equals("")) {
                    horario_g = all.trim().replace(" ", "").split(",");
                }
                // Se obtiene el horario especifico
                String h = horarios.getProperty(key.toString(), "");
                if (!h.equals("")) {
                    horario_e = horarios.get(key).toString().trim().replace(" ", "").split(",");
                }

                // Se crea el objeto canal respectivo
                Canal canal = new Canal(nombre, alias, origen, destino, activo, horario_g, horario_e);
                // Creación del hilo del canal
                if (canal.getActivo().equals(1)) {
                    LOG.debug("Creando hilo canal " + nombre + "...");
                    HiloCanal hilo_canal = new HiloCanal("Hilo " + nombre, canal, config, telegramNotifier);
                    LOG.debug(hilo_canal.getName() + " creado!");
                    LOG.debug("Ejecutando proceso de grabación del canal " + nombre + "...");
                    hilo_canal.start();
                }
            }
        } else {
            LOG.info("No hay canales registrados o activos para grabar en el archivo canales.properties!");
        }
    }

    private static String setBackslash(String cadena) {
        if (cadena != null && !cadena.equals("")) {
            int length = cadena.length();
            String backslash = cadena.substring(length - 1);
            if (!backslash.equals(DS)) {
                cadena += DS;
            }
        }
        return cadena;
    }

    private static void close(TelegramNotifier telegramNotifier) {
        LOG.debug("Enviando Mensaje y Terminando ejecución Recorder...");
        
        telegramNotifier.sendMessage("Terminando ejecucion Recorder...");
        // Espera un tiempo para asegurar que el mensaje se envíe antes de salir
        try {
            Thread.sleep(2000); // Puedes ajustar el tiempo de espera según sea necesario
        } catch (InterruptedException e) {
            LOG.warn("Error al pausar antes de salir: " + e.getMessage());
            telegramNotifier.sendMessage("Error al pausar antes de salir....");
        }
        System.exit(0);
    }

    private static void sendMessage(String botToken, String chatId, String messageText) {
        // URL del punto final de la API de Telegram para enviar mensajes
        String apiUrl = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        try {
            // Configuración de la conexión HTTP
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Creación del cuerpo del mensaje en formato JSON
            String mensaje = "{\"chat_id\":\"" + chatId + "\",\"text\":\"" + messageText + "\"}";

            // Envío de la solicitud con el cuerpo del mensaje
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = mensaje.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Lectura de la respuesta
            int responseCode = connection.getResponseCode();
            System.out.println("Código de respuesta: " + responseCode);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
