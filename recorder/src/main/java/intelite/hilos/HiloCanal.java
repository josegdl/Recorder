package intelite.hilos;

import static intelite.recorder.RecorderApp.DS;
import static intelite.recorder.RecorderApp.FFMPEG_PATH;
import static intelite.recorder.RecorderApp.ISLINUX;
import intelite.models.Canal;
import intelite.models.Config;
import intelite.recorder.TelegramNotifier;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import net.bramp.ffmpeg.progress.Progress;
import net.bramp.ffmpeg.progress.ProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class HiloCanal extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(HiloCanal.class);
    private final DateFormat df_anio = new SimpleDateFormat("yyyy");
    private final DateFormat df_mes = new SimpleDateFormat("MM");
    private final DateFormat df_dia = new SimpleDateFormat("dd");
    private final DateFormat df_hora = new SimpleDateFormat("HH");
    private final DateFormat df_min = new SimpleDateFormat("mm");
    private final DateFormat df_seg = new SimpleDateFormat("ss");
    private String nombre_archivo;
    private String destino_canal;

    private Canal canal;
    private Config config;

    // SCHEDULED EXECUTOR SERVICE 
    private ScheduledExecutorService scheduler;
    
    private TelegramNotifier telegramNotifier;
    
    public HiloCanal() {
        super();
    }

    public HiloCanal(String name, Canal canal, Config config, TelegramNotifier telegramNotifier) {
        super(name);
        this.canal = canal;
        this.config = config;
        this.telegramNotifier= telegramNotifier;
    }

    @Override
    public void run() {
        //Envia una notificacion al inicio
        telegramNotifier.sendMessage("El canal "+ canal.getNombre() + " ha iniciado. ");
        
        // Se obtienen los datos del canal a grabar
        if (canal.getAlias() != null && !canal.getAlias().equals("")) {
            destino_canal = canal.getDestino() + canal.getAlias() + DS;
            nombre_archivo = canal.getAlias() + "_";
        } else {
            destino_canal = canal.getDestino() + canal.getNombre() + DS;
            nombre_archivo = canal.getNombre() + "_";
        }

        // Se crea el scheduler
        scheduler = Executors.newScheduledThreadPool(1);

        // TAREA DE EMPAREJAMIENTO
        Long duration = config.getDur();
        long seg_now = ((LocalTime.now().getMinute()) * 60) + LocalTime.now().getSecond();
        long dur_empar = duration - (seg_now % duration);

        if (dur_empar == duration) {
            iniciarGrabacion(duration);
            telegramNotifier.sendMessage("Acaba de iniciar la grabacion....");
        } else {
            final Runnable emparejar = () -> {
                recorderProcess(dur_empar);
            };
            final ScheduledFuture<?> emparejarHandle = scheduler.scheduleAtFixedRate(emparejar, 0, dur_empar, TimeUnit.SECONDS);
            scheduler.schedule(() -> {
                emparejarHandle.cancel(true);
                try {
                    HiloCanal.sleep(1000); // Segundo de compensación
                } catch (InterruptedException e) {
                    LOG.warn("Exception metod run(): ", e);
                    Thread.currentThread().interrupt();
                }
                // Comienza grabación a duración específica
                iniciarGrabacion(duration);
            }, dur_empar - 1, TimeUnit.SECONDS);
        }
    }

    private void iniciarGrabacion(Long duration) {
        final Runnable grabar = () -> {
            recorderProcess(duration);
        };
        scheduler.scheduleAtFixedRate(grabar, 0, duration, TimeUnit.SECONDS);
    }

    private void recorderProcess(Long duration) {
        int maxReconnectAttempts = 3; // Máximo número de intentos de reconexión
        int reconnectAttempts = 0;
        boolean recordingSuccessful = false;

        while (!recordingSuccessful && reconnectAttempts < maxReconnectAttempts) {
            // Se verifica el horario
            if (validaHorario()) {
                String input = "";
                String output = "";
                Date date = new Date();
                String anio = df_anio.format(date);
                String mes = df_mes.format(date);
                String dia = df_dia.format(date);
                String hora = df_hora.format(date);
                String min = df_min.format(date);
                String seg = df_seg.format(date);

                if (canal.getOrigen() != null && !canal.getOrigen().equals("")) {
                    if (canal.getDestino() != null && !canal.getDestino().equals("")) {
                        input = canal.getOrigen();
                        output = getDestino(anio + mes + dia);
                    } else {
                        LOG.warn("No hay directorio de destino para las grabaciones del canal -> " + canal.getNombre());
                        telegramNotifier.sendMessage("No hay directorio de destino para las grabaciones del canal -> " + canal.getNombre());
                    }
                } else {
                    LOG.warn("No se encontró registrado el origen de la grabación -> " + canal.getNombre());
                    telegramNotifier.sendMessage("No se encontró registrado el origen de la grabación -> " + canal.getNombre());
                }

                if (!input.isEmpty() && !output.isEmpty()) {
                    String fileout = nombre_archivo + anio + mes + dia + "_" + hora + min + seg + "." + config.getFto();
                    String out = output + fileout;
                    try {
                        FFmpeg ffmpeg = new FFmpeg(FFMPEG_PATH + "rffmpeg");
                        FFmpegBuilder builder;

                        switch (config.getFto()) {
                            // Agrega aquí tus casos para otros formatos si es necesario
                            case "mkv":
                                builder = new FFmpegBuilder()
                                        .overrideOutputFiles(true)
                                        .addExtraArgs("-threads", "1")
                                        .setInput(input)
                                        .addOutput(out)
                                        .setDuration(duration, TimeUnit.SECONDS)
                                        .addExtraArgs("-drop_pkts_on_overflow", "1")
                                        .addExtraArgs("-attempt_recovery", "1")
                                        .addExtraArgs("-recover_any_error", "1")
                                        .addExtraArgs("-recovery_wait_time", "3")
                                        .addExtraArgs("-c", "copy")
                                        .done();
                                break;
                            case "mp3":
                                builder = new FFmpegBuilder()
                                        .setInput(input)
                                        .overrideOutputFiles(true)
                                        .addOutput(out)
                                        .disableVideo()
                                        .setAudioCodec("libmp3lame")
                                        .setAudioChannels(FFmpeg.AUDIO_STEREO)
                                        .setAudioBitRate(config.getAbr())
                                        .setAudioSampleRate(config.getAsr())
                                        .setDuration(duration, TimeUnit.SECONDS)
                                        .done();
                                break;
                            // Agrega más casos para otros formatos aquí
                            default:
                                // Manejo del caso por defecto
                                builder = new FFmpegBuilder()
                                        .setInput(input)
                                        .overrideOutputFiles(true)
                                        .addOutput(out)
                                        .setAudioChannels(FFmpeg.AUDIO_STEREO)
                                        .setAudioBitRate(config.getAbr())
                                        .setAudioSampleRate(config.getAsr())
                                        .setDuration(duration, TimeUnit.SECONDS)
                                        .done();
                                break;
                        }

                        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);

                        FFmpegJob job = executor.createJob(builder, new ProgressListener() {
                            final double duration_ns = duration * TimeUnit.SECONDS.toNanos(1);

                            @Override
                            public void progress(Progress progress) {
                                double percentage = progress.out_time_ns / duration_ns;
                                String line = String.format(
                                        "Creando archivo -> %s... %.0f%% [%s]",
                                        fileout,
                                        percentage * 100,
                                        progress.status
                                );
                                LOG.info(line);
                            }
                        });

                        job.run();
                        recordingSuccessful = true; // La grabación fue exitosa
                    } catch (IOException e) {
                        LOG.warn("Error en la grabación: " + e.getMessage());
                        telegramNotifier.sendMessage("Error en la grabacion..."+ canal.getNombre());
                        reconnectAttempts++;

                        if (reconnectAttempts < maxReconnectAttempts) {
                            //Envia una notificacion en caso de error 
                            telegramNotifier.sendMessage("Error an la grabacion del canal " + canal.getNombre() + ". Intentando reconectar en 60 segundos...");
                            LOG.info("Intentando reconectar en 60 segundos...");
                            try {
                                Thread.sleep(60000); // Pausa durante 60 segundos antes de intentar de nuevo
                            } catch (InterruptedException ex) {
                                LOG.warn("Error al pausar para reconexión: " + ex.getMessage());
                                telegramNotifier.sendMessage("Error al pausar para la reconexion: "+ canal.getNombre());
                            }
                        } else {
                            //Enciar una notificacion si se han agotado los intentos de reconexion 
                            telegramNotifier.sendMessage("Se han agotado los intentos de reconexion para el canal " + canal.getNombre() + ".");
                            LOG.warn("Se han agotado los intentos de reconexión.");
                        }
                    }
                }
            }
        }
    }

    private String getDestino(String carpeta_salida) {
        String dir_destino = destino_canal + carpeta_salida + DS;
        // Se verifica si existe la carpeta
        File carpeta_destino = new File(dir_destino);
        if (!carpeta_destino.exists()) {
            // Si no existe, se crea la carpeta de salida
            if (ISLINUX) {
//                Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxrwxrwx"); // 777
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr-xr-x"); // 755
                try {
                    Files.createDirectories(Paths.get(dir_destino), PosixFilePermissions.asFileAttribute(perms));
                    LOG.debug("Se ha creado la carpeta de destino -> " + dir_destino);
                    telegramNotifier.sendMessage("Se ha creado la carpeta de destino -> "+ dir_destino);
                } catch (IOException e) {
                    LOG.warn("No se pudo crear la carpeta de destino -> " + dir_destino);
                    telegramNotifier.sendMessage("No se pudo crear la carpeta de destino ->" + dir_destino);
                    LOG.warn("Error getDestino:", e);
                }
            } else {
                if (carpeta_destino.mkdirs()) {
                    LOG.debug("Se ha creado la carpeta de destino -> " + dir_destino);
                } else {
                    LOG.warn("No se pudo crear la carpeta de destino -> " + dir_destino);
                }
            }
        }
        return dir_destino;
    }

    public void stopScheduler() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(0, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            LOG.warn("Exception stopScheduler(): ", e);
        }
    }

    private boolean validaHorario() {
        // Se revisa el horario general
        if (canal.getHorario_g().length > 0) {
            for (String h : canal.getHorario_g()) {
                String[] data = h.split(">");
                // dia = data[0], horario = data[1]
                if (Integer.parseInt(data[0].trim()) == 8) {
                    String[] horas = data[1].trim().split("-");
//                    System.out.println("Hora INI = " + horas[0] + " Hora FIN = " + horas[1]);
                    int hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                    int ini = Integer.parseInt(horas[0]);
                    int fin = Integer.parseInt(horas[1]);
                    if (hora >= ini && hora < fin) {
                        LOG.debug("Horario (general) no habilitado para grabar canal " + canal.getNombre() + " > Esperando cambio de hora...");
                        return false;
                    }
                }
            }
        }
        // Se revisa el horario especifico
        if (canal.getHorario_e().length > 0) {
            for (String h : canal.getHorario_e()) {
                String[] data = h.split(">");
                if (Integer.parseInt(data[0].trim()) == Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                    String[] horas = data[1].trim().split("-");
//                    System.out.println("Hora INI = " + horas[0] + " Hora FIN = " + horas[1]);
                    int hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                    int ini = Integer.parseInt(horas[0]);
                    int fin = Integer.parseInt(horas[1]);
                    if (hora >= ini && hora < fin) {
                        LOG.debug("Horario (específico) no habilitado para grabar canal " + canal.getNombre() + " > Esperando cambio de hora...");
                        return false;
                    }
                }
            }
        }
        return true;
    }

}
