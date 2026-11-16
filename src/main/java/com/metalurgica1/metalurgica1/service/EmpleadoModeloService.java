package com.metalurgica1.metalurgica1.service;

import com.metalurgica1.metalurgica1.DTO.EmpleadoModeloDTO;
import com.metalurgica1.metalurgica1.modelo.Empleado;
import com.metalurgica1.metalurgica1.modelo.Empleado_Gerente;
import com.metalurgica1.metalurgica1.modelo.Empleado_modelo;
import com.metalurgica1.metalurgica1.modelo.enums.EEstadoActividad;
import com.metalurgica1.metalurgica1.repositorio.IEmpleadoRepository;
import com.metalurgica1.metalurgica1.repositorio.IGerenteRepository;
import com.metalurgica1.metalurgica1.service.Excepciones.EmpleadoNoEncontradoException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class EmpleadoModeloService {

    @Autowired
    private IEmpleadoRepository iEmpleadoRepository;
    @Autowired
    private IGerenteRepository iGerenteRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public EmpleadoModeloService(IEmpleadoRepository iEmpleadoRepository, IGerenteRepository iGerenteRepository) {
        this.iEmpleadoRepository = iEmpleadoRepository;
        this.iGerenteRepository = iGerenteRepository;
    }

    public List<EmpleadoModeloDTO> listarTodosEmpleados() {
            List<Empleado> empleados = iEmpleadoRepository.findAll();
            List<Empleado_Gerente> gerentes = iGerenteRepository.findAll();
            List<Empleado_modelo> nuevaLista =
                    Stream
                    .concat(empleados.stream(),gerentes.stream())
                    .filter(p->p.getEEstadoActividad() == EEstadoActividad.ACTIVO)
                    .sorted(Comparator.comparing(Empleado_modelo::getLegajo))
                    .toList();

            return nuevaLista.stream().map(p-> new EmpleadoModeloDTO(p.getLegajo(),p.getEmail(),p.getNombre(),p.getTelefono(),p.getDni(),p.getEtiquetaDeAcceso())).toList();
        }

        public EmpleadoModeloDTO buscarEmpleadoPorId(Long legajo){
            return listarTodosEmpleados().stream().filter(p->p.legajo().equals(legajo)).findFirst().orElseThrow(()->new RuntimeException("Empleado con id"+ legajo+" no encontrado"));
        }

        public Empleado_modelo buscarEntidadPorLegajo(Long legajo){
            Empleado_modelo empleado = entityManager.find(Empleado_modelo.class, legajo);

            if(empleado == null || empleado.getEEstadoActividad() != EEstadoActividad.ACTIVO){
                throw new RuntimeException("Empleado con id "+ legajo +" no encontrado");
            }

            return empleado;
        }

        public Optional<EmpleadoModeloDTO> buscarEmpleadoPorEmail(String email){
            return listarTodosEmpleados().stream().filter(p->p.email().equals(email)).findFirst();
        }
}
