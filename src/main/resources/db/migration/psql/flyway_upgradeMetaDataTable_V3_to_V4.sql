DROP INDEX "schema_version_vr_idx";
DROP INDEX "schema_version_ir_idx";
ALTER TABLE "schema_version" DROP COLUMN "version_rank";
ALTER TABLE "schema_version" DROP CONSTRAINT "schema_version_pk";
ALTER TABLE "schema_version" ALTER COLUMN "version" DROP NOT NULL;
ALTER TABLE "schema_version" ADD CONSTRAINT "schema_version_pk" PRIMARY KEY ("installed_rank");
UPDATE "schema_version" SET "type"='BASELINE' WHERE "type"='INIT';
