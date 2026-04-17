ALTER TABLE orders
    ALTER COLUMN opened_by_user_id DROP NOT NULL;

ALTER TABLE sales
    ALTER COLUMN opened_by_user_id DROP NOT NULL;

ALTER TABLE sales
    ALTER COLUMN closed_by_user_id DROP NOT NULL;
