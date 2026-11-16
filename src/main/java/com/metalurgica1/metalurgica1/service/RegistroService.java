package com.metalurgica1.metalurgica1.service;

import com.metalurgica1.metalurgica1.DTO.*;
import com.metalurgica1.metalurgica1.modelo.*;
import com.metalurgica1.metalurgica1.modelo.enums.EEstadoActividad;
import com.metalurgica1.metalurgica1.modelo.enums.EEtiquetaDeAcceso;
import com.metalurgica1.metalurgica1.modelo.enums.EProceso;
import com.metalurgica1.metalurgica1.repositorio.IClienteRepository;
import com.metalurgica1.metalurgica1.repositorio.IRegistroRepository;
import com.metalurgica1.metalurgica1.repositorio.ITareaRepository;
import com.metalurgica1.metalurgica1.service.Excepciones.ClienteNoEncontradoException;
import com.metalurgica1.metalurgica1.service.Excepciones.RegistroNoEncontradoException;
import com.metalurgica1.metalurgica1.service.Excepciones.TareaNoEncontradaExeption;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RegistroService {
    private final IRegistroRepository iRegistroRepository;
    private final IClienteRepository iClienteRepository;
    private final ITareaRepository iTareaRepository;
    private final EmpleadoModeloService empleadoModeloService;
    private final NotificacionClienteService notificacionClienteService;

    public RegistroService(NotificacionClienteService notificacionClienteService, EmpleadoModeloService empleadoModeloService, ITareaRepository iTareaRepository, IClienteRepository iClienteRepository, IRegistroRepository iRegistroRepository) {
        this.notificacionClienteService = notificacionClienteService;
        this.empleadoModeloService = empleadoModeloService;
        this.iTareaRepository = iTareaRepository;
        this.iClienteRepository = iClienteRepository;
        this.iRegistroRepository = iRegistroRepository;
    }

    private RegistroDTO convertirADTO(Registro r) {
        return new RegistroDTO(r.getId(), r.getTitulo(), r.getTarea().getId(),
                r.getCliente().getIdCliente(), r.getEProceso(), listarIdEmpleados(r), r.getPublicado());
    }

    public List<RegistroDTO> listarRegistros() {
        return iRegistroRepository.
                findAll()
                .stream()
                .filter(p -> p.getEEstadoActividad() == EEstadoActividad.ACTIVO)
                .map(r -> new RegistroDTO(
                        r.getId(),
                        r.getTitulo(),
                        r.getTarea().getId(),
                        r.getCliente().getIdCliente(),
                        r.getEProceso(),
                        listarIdEmpleados(r),
                        r.getPublicado()))
                .collect(Collectors.toList());
    }

    public List<RegistroDTO> listarRegistrosClientes(Long id) {
        return iRegistroRepository
                .findAll()
                .stream()
                .filter(p -> (p.getEEstadoActividad() == EEstadoActividad.ACTIVO) && (p.getCliente().getIdCliente().equals(id)))
                .map(r -> new RegistroDTO(
                        r.getId(),
                        r.getTitulo(),
                        r.getTarea().getId(),
                        r.getCliente().getIdCliente(),
                        r.getEProceso(),
                        listarIdEmpleados(r),
                        r.getPublicado()))
                .toList();
    }

    public List<Empleado_modelo> listarEmpleados() {
        return iRegistroRepository.findAll().stream().flatMap(registro -> registro.getParticipantes().stream()).toList();

    }

    public List<TareaDTO> listarTareasRegistro(Long id) {
        return iRegistroRepository.findById(id)
                .stream()
                .filter(reg -> reg.getEEstadoActividad() == EEstadoActividad.ACTIVO)
                .map(Registro::getTarea)
                .filter(tarea -> tarea != null && tarea.getEEstadoActividad() == EEstadoActividad.ACTIVO)
                .map(t -> new TareaDTO(
                        t.getId(),
                        t.getCategorias(),
                        t.getTitulo(),
                        t.getFechaDeEntrega(),
                        t.getFechaDeRegistro(),
                        t.getDescripcionMaterial(),
                        t.getDescripcionGeneral(),
                        t.getCantidad()
                ))
                .toList();
    }

    public RegistroDTO buscarRegistroPorId(Long id) throws RegistroNoEncontradoException {
        Registro r = iRegistroRepository.findById(id)
                .orElseThrow(() -> new RegistroNoEncontradoException(id));

        return new RegistroDTO(
                r.getId(),
                r.getTitulo(),
                r.getTarea().getId(),
                r.getCliente().getIdCliente(),
                r.getEProceso(),
                listarIdEmpleados(r),
                r.getPublicado());
    }

    public List<RegistroDTO> buscarPublicados() {
        return iRegistroRepository.findByPublicadoTrue()
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    public List<RegistroDTO> buscarPorProceso(EProceso proceso) {
        return iRegistroRepository.findByeProceso(proceso)
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    private List<Empleado_modelo> listarEmpleados(CrearRegistroDTO dto) {
        return dto.participantesId().stream()
                .map(empleadoModeloService::buscarEntidadPorLegajo)
                .toList();
    }

    private List<Long> listarIdEmpleados(Registro r) {
        return r.getParticipantes().stream()
                .map(Empleado_modelo::getLegajo)
                .toList();
    }


    @Transactional
    public CrearRegistroDTO crearRegistro(CrearRegistroDTO dto) throws TareaNoEncontradaExeption, ClienteNoEncontradoException {
        Registro registro = new Registro();

        Tarea t = iTareaRepository.findById(dto.tareaId())
                .orElseThrow(() -> new TareaNoEncontradaExeption(dto.tareaId()));

        Cliente c = iClienteRepository.findById(dto.clienteId())
                .orElseThrow(() -> new ClienteNoEncontradoException(dto.clienteId()));

        registro.setEProceso(EProceso.ESPERA);

        registro.setTitulo(dto.titulo());

        registro.setTarea(t);

        registro.setCliente(c);

        registro.setEEstadoActividad(EEstadoActividad.ACTIVO);

        registro.setParticipantes(listarEmpleados(dto));

        Registro nuevoRegistro = iRegistroRepository.save(registro);

        return new CrearRegistroDTO(
                nuevoRegistro.getTitulo(),
                nuevoRegistro.getTarea().getId(),
                nuevoRegistro.getCliente().getIdCliente(),
                nuevoRegistro.getEProceso(),
                listarIdEmpleados(nuevoRegistro),
                nuevoRegistro.getPublicado());
    }

    @Transactional
    public CrearRegistroDTO modificarRegistro(Long id, CrearRegistroDTO dto) throws RegistroNoEncontradoException, TareaNoEncontradaExeption, ClienteNoEncontradoException {
        Registro registro = iRegistroRepository.findById(id)
                .orElseThrow(() -> new RegistroNoEncontradoException(id));

        Tarea t = iTareaRepository.findById(dto.tareaId())
                .orElseThrow(() -> new TareaNoEncontradaExeption(dto.tareaId()));

        Cliente c = iClienteRepository.findById(dto.clienteId())
                .orElseThrow(() -> new ClienteNoEncontradoException(dto.clienteId()));

        EProceso anterior = registro.getEProceso();

        registro.setTitulo(dto.titulo());
        registro.setEProceso(dto.proceso());
        registro.setTarea(t);
        registro.setCliente(c);

        if (dto.participantesId() != null && !dto.participantesId().isEmpty()) {
            List<Empleado_modelo> participantes = listarEmpleados(dto);

            registro.setParticipantes(participantes);
        }

        Registro nuevoRegistro = iRegistroRepository.save(registro);

        if (anterior != nuevoRegistro.getEProceso()) {
            notificacionClienteService.notificarCambioEstado(
                    new NotificacionClienteService.DatosCambioEstado(
                            nuevoRegistro.getId(),
                            nuevoRegistro.getTitulo(),
                            nuevoRegistro.getCliente().getIdCliente(),
                            nuevoRegistro.getCliente().getEmail(),
                            nuevoRegistro.getCliente().getNombre(),
                            nuevoRegistro.getTarea().getTitulo(),
                            anterior,
                            nuevoRegistro.getEProceso()
                    )
            );
        }

        return new CrearRegistroDTO(
                nuevoRegistro.getTitulo(),
                nuevoRegistro.getTarea().getId(),
                nuevoRegistro.getCliente().getIdCliente(),
                nuevoRegistro.getEProceso(),
                listarIdEmpleados(nuevoRegistro),
                nuevoRegistro.getPublicado());
    }

    @Transactional
    public RegistroDTO publicarRegistro(Long id) {
        Registro registro = iRegistroRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro no encontrado"));
        registro.setPublicado(true);

        Registro guardado = iRegistroRepository.save(registro);

        return convertirADTO(guardado);
    }

    public void eliminarRegistro(Long id) throws RegistroNoEncontradoException {
        Registro r = iRegistroRepository.findById(id).orElseThrow(() -> new RegistroNoEncontradoException(id));
        r.setEEstadoActividad(EEstadoActividad.CANCELADO);
        iRegistroRepository.save(r);
    }
}
