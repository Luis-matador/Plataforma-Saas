package com.example.plataformaSaaS.service;

import com.example.plataformaSaaS.dto.MetodoPagoResumenDTO;
import com.example.plataformaSaaS.model.*;
import com.example.plataformaSaaS.model.enums.TipoPago;
import com.example.plataformaSaaS.repository.MetodoPagoRepository;
import com.example.plataformaSaaS.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class MetodoPagoService {

    private final MetodoPagoRepository metodoPagoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<MetodoPago> listarPorUsuario(Long usuarioId) {
        return metodoPagoRepository.findByUsuarioId(usuarioId);
    }

    @Transactional(readOnly = true)
    public List<MetodoPagoResumenDTO> listarResumenPorUsuario(Long usuarioId) {
        return listarPorUsuario(usuarioId).stream().map(m -> {
            MetodoPagoResumenDTO.MetodoPagoResumenDTOBuilder builder = MetodoPagoResumenDTO.builder()
                    .id(m.getId())
                    .tipoPago(m.getTipoPago());
            if (m instanceof PagoTarjeta t) {
                builder.numeroTarjeta(t.getNumeroTarjeta())
                       .titular(t.getTitular())
                       .fechaExpiracion(t.getFechaExpiracion());
            } else if (m instanceof PagoPayPal p) {
                builder.emailPaypal(p.getEmailPaypal());
            } else if (m instanceof PagoTransferencia tr) {
                builder.numeroCuenta(tr.getNumeroCuenta())
                       .banco(tr.getBanco());
            }
            return builder.build();
        }).toList();
    }

    @Transactional(readOnly = true)
    public Optional<MetodoPago> buscarPorId(Long id) {
        return metodoPagoRepository.findById(id);
    }

    public MetodoPago guardarTarjeta(Usuario usuario, String numeroTarjeta, String titular, String fechaExpiracion) {
        PagoTarjeta pago = new PagoTarjeta();
        pago.setTipoPago(TipoPago.TARJETA);
        pago.setUsuario(usuario);
        pago.setActivo(true);
        pago.setNumeroTarjeta(numeroTarjeta.length() > 4 ? numeroTarjeta.substring(numeroTarjeta.length() - 4) : numeroTarjeta);
        pago.setTitular(titular);
        pago.setFechaExpiracion(fechaExpiracion);
        return metodoPagoRepository.save(pago);
    }

    public MetodoPago guardarTarjetaPorUsuarioId(Long usuarioId, String numeroTarjeta, String titular, String fechaExpiracion) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        return guardarTarjeta(usuario, numeroTarjeta, titular, fechaExpiracion);
    }

    public MetodoPago guardarPayPal(Usuario usuario, String emailPaypal) {
        PagoPayPal pago = new PagoPayPal();
        pago.setTipoPago(TipoPago.PAYPAL);
        pago.setUsuario(usuario);
        pago.setActivo(true);
        pago.setEmailPaypal(emailPaypal);
        return metodoPagoRepository.save(pago);
    }

    public MetodoPago guardarPayPalPorUsuarioId(Long usuarioId, String emailPaypal) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        return guardarPayPal(usuario, emailPaypal);
    }

    public MetodoPago guardarTransferencia(Usuario usuario, String numeroCuenta, String banco) {
        PagoTransferencia pago = new PagoTransferencia();
        pago.setTipoPago(TipoPago.TRANSFERENCIA);
        pago.setUsuario(usuario);
        pago.setActivo(true);
        pago.setNumeroCuenta(numeroCuenta);
        pago.setBanco(banco);
        return metodoPagoRepository.save(pago);
    }

    public MetodoPago guardarTransferenciaPorUsuarioId(Long usuarioId, String numeroCuenta, String banco) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        return guardarTransferencia(usuario, numeroCuenta, banco);
    }

    public void eliminar(Long id) {
        metodoPagoRepository.deleteById(id);
    }

    public void eliminarPorUsuario(Long metodoId, Long usuarioId) {
        MetodoPago metodo = metodoPagoRepository.findById(metodoId)
                .orElseThrow(() -> new IllegalArgumentException("Metodo de pago no encontrado"));
        if (!metodo.getUsuario().getId().equals(usuarioId)) {
            throw new IllegalArgumentException("No puedes eliminar este metodo de pago");
        }
        metodoPagoRepository.deleteById(metodoId);
    }
}
