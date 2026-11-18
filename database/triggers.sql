CREATE TRIGGER IF NOT EXISTS packaging_guard_insert BEFORE INSERT ON packaging BEGIN
  SELECT CASE WHEN COALESCE((SELECT quality_status FROM honey_batch WHERE batch_id=NEW.batch_id),'missing') <> 'accepted' THEN RAISE(ABORT,'Batch not accepted or missing') END;
  SELECT CASE WHEN NEW.container_volume_g * NEW.quantity + COALESCE((SELECT SUM(container_volume_g * quantity) FROM packaging WHERE batch_id=NEW.batch_id),0) > (SELECT gross_weight_kg * 1000 FROM honey_batch WHERE batch_id=NEW.batch_id) THEN RAISE(ABORT,'Insufficient honey') END;
END;
CREATE TRIGGER IF NOT EXISTS packaging_guard_update BEFORE UPDATE ON packaging BEGIN
  SELECT CASE WHEN COALESCE((SELECT quality_status FROM honey_batch WHERE batch_id=NEW.batch_id),'missing') <> 'accepted' THEN RAISE(ABORT,'Batch not accepted or missing') END;
  SELECT CASE WHEN NEW.container_volume_g * NEW.quantity + COALESCE((SELECT SUM(container_volume_g * quantity) FROM packaging WHERE batch_id=NEW.batch_id AND packaging_id<>OLD.packaging_id),0) > (SELECT gross_weight_kg * 1000 FROM honey_batch WHERE batch_id=NEW.batch_id) THEN RAISE(ABORT,'Insufficient honey') END;
  SELECT CASE WHEN NEW.quantity < COALESCE((SELECT SUM(s.quantity) FROM sale_item s JOIN customer_order o ON o.order_id=s.order_id WHERE s.packaging_id=OLD.packaging_id AND o.status<>'cancelled'),0) THEN RAISE(ABORT,'Quantity below sold stock') END;
END;
CREATE TRIGGER IF NOT EXISTS batch_guard_update BEFORE UPDATE OF gross_weight_kg,quality_status ON honey_batch BEGIN
  SELECT CASE WHEN NEW.quality_status <> 'accepted' AND EXISTS(SELECT 1 FROM packaging WHERE batch_id=OLD.batch_id) THEN RAISE(ABORT,'Batch already packaged') END;
  SELECT CASE WHEN NEW.gross_weight_kg * 1000 < COALESCE((SELECT SUM(container_volume_g*quantity) FROM packaging WHERE batch_id=OLD.batch_id),0) THEN RAISE(ABORT,'Weight below packaged amount') END;
END;
CREATE TRIGGER IF NOT EXISTS sale_guard_insert BEFORE INSERT ON sale_item BEGIN
  SELECT CASE WHEN NEW.quantity > COALESCE((SELECT available_quantity FROM v_product_balance WHERE packaging_id=NEW.packaging_id),-1) AND COALESCE((SELECT status FROM customer_order WHERE order_id=NEW.order_id),'missing') <> 'cancelled' THEN RAISE(ABORT,'Insufficient stock or product missing') END;
END;
CREATE TRIGGER IF NOT EXISTS sale_guard_update BEFORE UPDATE ON sale_item BEGIN
  SELECT CASE WHEN COALESCE((SELECT status FROM customer_order WHERE order_id=NEW.order_id),'missing') <> 'cancelled' AND NEW.quantity > COALESCE((SELECT available_quantity FROM v_product_balance WHERE packaging_id=NEW.packaging_id),-1) + CASE WHEN OLD.packaging_id=NEW.packaging_id AND (SELECT status FROM customer_order WHERE order_id=OLD.order_id)<>'cancelled' THEN OLD.quantity ELSE 0 END THEN RAISE(ABORT,'Insufficient stock or product missing') END;
END;
CREATE TRIGGER IF NOT EXISTS order_guard_reactivate BEFORE UPDATE OF status ON customer_order WHEN OLD.status='cancelled' AND NEW.status<>'cancelled' BEGIN
  SELECT CASE WHEN EXISTS(SELECT 1 FROM sale_item s JOIN v_product_balance b ON b.packaging_id=s.packaging_id WHERE s.order_id=OLD.order_id AND s.quantity>b.available_quantity) THEN RAISE(ABORT,'Insufficient stock to reactivate order') END;
END;
